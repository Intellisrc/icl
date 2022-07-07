package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import com.intellisrc.db.auto.Table
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.auto.Table.getColumnName
import static com.intellisrc.db.jdbc.Derby.SubProtocol.*

/**
 * Derby Apache Database (JavaDB)
 *
 * Derby has a small footprint -- about 3.5 megabytes for the base engine and embedded JDBC driver.
 * Derby is based on the Java, JDBC, and SQL standards.
 * Derby provides an embedded JDBC driver that lets you embed Derby in any Java-based solution.
 * One advantage over SQLite is that can be run as a server and encryption can be used
 *
 * @since 17/12/14.
 *
 * Additional settings:
 * db.derby.params = [:]
 */
@CompileStatic
class Derby extends JDBCServer implements AutoJDBC {
    static enum SubProtocol {
        SERVER, DIRECTORY, MEMORY, CLASSPATH, JAR
    }
    String dbname = ""
    String user = ""
    String password = ""
    String hostname = "localhost"
    int port = 1527
    String driver = "org.apache.derby.jdbc.EmbeddedDriver"
    boolean create = false
    boolean encrypt = false
    boolean memory = Config.get("db.derby.memory", false)
    SubProtocol subProtocol = DIRECTORY

    // Derby specific parameters:
    // https://db.apache.org/derby/docs/10.0/manuals/reference/sqlj238.html#HDRSII-ATTRIB-24612
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.derby.params", [
            create : create
            // encryptionProvider :
            // encryptionAlgorithm :
            // logDevice :
            // rollForwardRecoveryFrom :
            // createFrom :
            // restoreFrom :
            // shutdown :
        ] + params)
    }

    @Override
    String getConnectionString() {
        String sub = ""
        // Do not set unless is enabled:
        if(encrypt) {
            parameters.dataEncryption = true
        }
        if(memory) {
            subProtocol = MEMORY
        }
        //noinspection GroovyFallthrough
        switch (subProtocol) {
            case SERVER: // Not used
            case DIRECTORY:
                // empty
                break
            case MEMORY:
                sub = "memory:"
                break
            case CLASSPATH:
            case JAR:
                // Must start with /
                // https://db.apache.org/derby/docs/10.8/devguide/cdevdvlp17453.html
                if(! dbname.startsWith("/")) {
                    dbname = "/${dbname}"
                }
                break
        }
        return "derby:" + (subProtocol == SERVER ? "//$hostname:$port/$dbname" : "${sub}$dbname") + ";" +
                parameters.collect {
                    "${it.key}=${it.value}"
                }.join(";")
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String catalogSearchName = "%"
    String schemaSearchName = "%"
    boolean supportsReplace = false

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }
    ////////////////////////////// AUTO ////////////////////////////////////
    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, List<Table.ColumnDB> columns) {
        boolean ok
        String createSQL = "CREATE TABLE IF NOT EXISTS `${tableName}` (\n"
        List<String> defs = []
        List<String> keys = []
        Map<String, List<String>> uniqueGroups = [:]
        columns.each {
            Table.ColumnDB column ->
                List<String> parts = ["`${column.name}`".toString()]
                if (column.annotation.columnDefinition()) {
                    parts << column.annotation.columnDefinition()
                } else {
                    String type = getColumnDefinition(column.type, column.annotation)
                    parts << type

                    if (column.defaultVal) {
                        parts << getDefaultQuery(column.defaultVal, column.annotation.nullable())
                    }

                    List<String> extra = []
                    if (column.annotation.unique() || column.annotation.uniqueGroup()) {
                        if (column.annotation.uniqueGroup()) {
                            if (!uniqueGroups.containsKey(column.annotation.uniqueGroup())) {
                                uniqueGroups[column.annotation.uniqueGroup()] = []
                            }
                            uniqueGroups[column.annotation.uniqueGroup()] << column.name
                        } else {
                            extra << "UNIQUE"
                        }
                    }
                    if (!extra.empty) {
                        parts.addAll(extra)
                    }
                }
                if (column.annotation.key()) {
                    keys << "KEY `${tableName}_${column.name}_key_index` (`${column.name}`)".toString()
                }
                defs << parts.join(' ')
        }
        if (!keys.empty) {
            defs.addAll(keys)
        }
        if (!uniqueGroups.keySet().empty) {
            uniqueGroups.each {
                defs << "UNIQUE KEY `${tableName}_${it.key}` (`${it.value.join('`, `')}`)".toString()
            }
        }
        String fks = columns.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }
        if (engine) {
            engine = "ENGINE=${engine}"
        }
        createSQL += defs.join(",\n") + "\n) ${engine} CHARACTER SET=${charset}\nCOMMENT='v.${version}'"
        ok = db.set(new Query(createSQL))
        if(!ok) {
            Log.v(createSQL)
            Log.e("Unable to create table.")
        }
        return ok
    }

    @Override
    String getColumnDefinition(Class cType, Column column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (cType) {
            case boolean:
            case Boolean:
                type = "BOOLEAN"
                break
            case Inet4Address:
                type = "VARCHAR(${column?.length() ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column?.length() ?: 45})"
                break
            case String:
                type = column?.length() > 32672 ? "CLOB" : "VARCHAR(${column?.length() ?: 255})"
                break
                // All numeric values share unsigned/autoincrement and primary instructions:
            case byte:
            case short:
                type = type ?: "SMALLINT"
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: "INTEGER"
            case BigInteger:
            case long:
            case Long:
                type = type ?: "BIGINT"
                boolean hasAnnotation = column != null
                int len = hasAnnotation ? column.length() : 0
                String length = len ? "(${len})" : ""
                boolean unsignedDefault = Column.class.getMethod("unsigned").defaultValue
                boolean autoIncDefault = Column.class.getMethod("autoincrement").defaultValue
                boolean primaryDefault = Column.class.getMethod("primary").defaultValue
                List<String> extra = [type, length]
                extra << ((hasAnnotation ? column.unsigned() : unsignedDefault) ? "UNSIGNED" : "")
                extra << ((hasAnnotation ? column.primary() && column.autoincrement() : autoIncDefault) ? "AUTO_INCREMENT" : "")
                extra << ((hasAnnotation ? column.primary() : primaryDefault) ? "PRIMARY KEY" : "")
                type = extra.findAll {it }.join(" ")
                break
            case float:
            case Float:
            case double:
            case Double:
            case BigDecimal:
                type = "FLOAT"
                break
            case LocalDate:
                type = "DATE"
                break
            case LocalDateTime:
                type = "TIMESTAMP"
                break
            case LocalTime:
                type = "TIME"
                break
            case URL:
            case URI:
            case Collection:
            case Map:
                type = column?.key() || column?.unique() || (column?.length() ?: 256) <= 255 ? "VARCHAR(${column?.length() ?: 255})" : "TEXT"
                break
            case Enum:
                type = "VARCHAR"
                break
            case byte[]:
                type = "BLOB"
                break
            default:
                // Having a constructor with String or Having a static method 'fromString'
                boolean canImport = false
                try {
                    cType.getConstructor(String.class)
                    canImport = true
                } catch(Exception ignore) {
                    try {
                        Method method = cType.getDeclaredMethod("fromString", String.class)
                        canImport = Modifier.isStatic(method.modifiers) && method.returnType == cType
                    } catch(Exception ignored) {}
                }
                if(canImport) {
                    int len = column?.length() ?: 256
                    type = len < 256 ? "VARCHAR($len)" : "TEXT"
                } else {
                    Log.w("Unknown field type: %s", cType.simpleName)
                    Log.d("If you want to able to use '%s' type in the database, either set `fromString` " +
                        "as static method or set a constructor which accepts `String`", cType.simpleName)
                }
        }
        return type
    }

    @Override
    String getForeignKey(String tableName, Table.ColumnDB column) {
        String indices = ""
        switch (column.type) {
            case Model:
                Constructor<?> ctor = column.type.getConstructor()
                Model refType = (ctor.newInstance() as Model)
                String joinTable = refType.tableName
                String action = column.annotation ? column.annotation.ondelete().toString() : Column.class.getMethod("ondelete").defaultValue.toString()
                indices = "CONSTRAINT `${tableName}_${column.name}_fk` FOREIGN KEY (`${column.name}`) " +
                    "REFERENCES `${joinTable}`(`${getColumnName(refType.pk)}`) ON DELETE ${action}"
                break
        }
        return indices
    }

}