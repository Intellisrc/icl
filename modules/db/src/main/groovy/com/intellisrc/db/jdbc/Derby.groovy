package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.*

import static com.intellisrc.db.auto.Table.ColumnDB
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
    boolean encrypt = Config.get("db.derby.encrypt", false)
    boolean memory = Config.get("db.derby.memory", false)
    boolean create = memory ?: Config.get("db.derby.create", false) //If in memory it will create automatically
    String tableMeta = Config.get("db.derby.meta", "sys_meta")
    SubProtocol subProtocol = DIRECTORY

    // QUERY BUILDING -------------------------
    // Query parameters
    String catalogSearchName = "%"
    String schemaSearchName = "%"
    boolean supportsReplace = false
    boolean supportsBoolean = true

    // Derby specific parameters:
    // https://db.apache.org/derby/docs/10.0/manuals/reference/sqlj238.html#HDRSII-ATTRIB-24612
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        if(create) {
            params.create = create
        }
        return Config.get("db.derby.params", [ :
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
                if(dbname.empty) {
                    dbname = "default"
                }
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
        return "derby:" + (subProtocol == SERVER ? "//$hostname:$port/$dbname" : "${sub}$dbname") + (parameters.isEmpty() ? "" : ";" +
                parameters.collect {
                    "${it.key}=${it.value}"
                }.join(";"))
    }

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }
    ////////////////////////////// AUTO ////////////////////////////////////
    boolean exists(DB db, String tableName) {
        return get(db, "SELECT TRUE FROM SYS.SYSTABLES WHERE TABLENAME = '${tableName.toUpperCase()}' AND TABLETYPE = 'T'").toBool()
    }
    @Override
    void autoInit(DB db) {
        if(! exists(db, tableMeta)) {
            db.set(new Query("CREATE TABLE $tableMeta (" +
                "table_name VARCHAR(50) PRIMARY KEY," +
                "version INT NOT NULL DEFAULT 1" +
                ")"))
        }
    }
    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, List<ColumnDB> columns) {
        boolean ok = false
        if(! exists(db, tableName)) {
            String createSQL = "CREATE TABLE ${tableName} (\n"
            List<String> defs = []
            List<String> keys = []
            Map<String, List<String>> uniqueGroups = [:]
            List<ColumnDB> pks = columns.findAll { it.annotation.primary() }
            if(pks.size() > 1) {
                pks.each { it.multipleKey = true }
            }
            columns.each {
                ColumnDB column ->
                    List<String> parts = ["${column.name}".toString()]
                    if (column.annotation.columnDefinition()) {
                        parts << column.annotation.columnDefinition()
                    } else {
                        String type = getColumnDefinition(column).replace("_pk", tableName + "_pk" + "_v" + version)
                        parts << type
                        // Default value
                        parts << getDefaultQuery(column)

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
                    defs << parts.join(' ')
            }
            if (!keys.empty) {
                defs.addAll(keys)
            }
            if (pks.size() > 1) {
                defs << ("PRIMARY KEY (" + (pks.collect {"${ it.name }" }).join(",") + ")")
            }
            if (!uniqueGroups.keySet().empty) {
                uniqueGroups.each {
                    defs << "UNIQUE KEY ${tableName}_${it.key} (${it.value.join(', ')})".toString()
                }
            }
            String fks = columns.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
            if (fks) {
                defs << fks
            }
            createSQL += defs.join(",\n") + "\n)"
            ok = db.set(new Query(createSQL))
            if (!ok) {
                Log.v(createSQL)
                Log.e("Unable to create table.")
            }
        }
        return ok
    }

    @Override
    boolean turnFK(final DB db, boolean on) {
        return true //Not supported: https://www.mail-archive.com/derby-user@db.apache.org/msg05345.html
    }
    @Override
    boolean copyTableDesc(final DB db, String from, String to) {
        return set(db, "CREATE TABLE ${to} AS SELECT * FROM ${from} WITH NO DATA")
    }
    @Override
    boolean copyTableData(DB db, String from, String to, List<ColumnDB> columns) {
        boolean ok = set(db, "INSERT INTO ${to} SELECT * FROM ${from}")
        ColumnDB ai = columns.find { it.annotation.autoincrement() }
        if(ai) {
            int max = get(db, "SELECT (MAX(${ai.name}) + 1) AS m FROM ${from}").toInt()
            set(db, "ALTER TABLE ${to} ALTER COLUMN ${ai.name} RESTART WITH $max")
        }
        return ok
    }
    @Override
    boolean setVersion(final DB db, String dbname, String table, int version) {
        return db.table(tableMeta).replace([
            table_name : table,
            version : version
        ])
    }
    @Override
    int getVersion(final DB db, String dbname, String table) {
        return db.table(tableMeta).field("version").get(table_name : table).toInt()
    }


    @Override
    String getColumnDefinition(ColumnDB column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = "BOOLEAN"
                break
            case Inet4Address:
                type = "VARCHAR(${column.annotation.length() ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column.annotation.length() ?: 45})"
                break
            case String:
                type = column.annotation.length() > 32672 ? "CLOB" : "VARCHAR(${column.annotation.length() ?: 255})"
                break
                // All numeric values share unsigned/autoincrement and primary instructions:
            case byte:
            case short:
                type = type ?: "SMALLINT"
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: "INT"
            case BigInteger:
            case long:
            case Long:
                type = type ?: "BIGINT"
                int len = column.annotation.length()
                String length = len ? "(${len})" : ""
                List<String> extra = [type, length]
                extra << (column.annotation.primary() && column.annotation.autoincrement() ? "GENERATED BY DEFAULT AS IDENTITY" : "")
                extra << (! column.multipleKey && column.annotation.primary() ? "PRIMARY KEY" : "")
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
                type = column.annotation.key() || column.annotation.unique() || (column.annotation.length() ?: 256) <= 255 ? "VARCHAR(${column.annotation.length() ?: 255})" : "CLOB"
                break
            case Enum:
                int maxLen = column.type.getEnumConstants().toList().max { it.toString().length() }.toString().length()
                type = "VARCHAR(${maxLen})"
                break
            case byte[]:
                type = "BLOB"
                break
            default:
                // Having a constructor with String or Having a static method 'fromString'
                boolean canImport = false
                try {
                    column.type.getConstructor(String.class)
                    canImport = true
                } catch(Exception ignore) {
                    try {
                        Method method = column.type.getDeclaredMethod("fromString", String.class)
                        canImport = Modifier.isStatic(method.modifiers) && method.returnType == column.type
                    } catch(Exception ignored) {}
                }
                if(canImport) {
                    int len = column.annotation.length() ?: 256
                    type = len < 256 ? "VARCHAR($len)" : "CLOB"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.d("If you want to able to use '%s' type in the database, either set fromString " +
                        "as static method or set a constructor which accepts String", column.type.simpleName)
                }
        }
        return type
    }

    @Override
    String getForeignKey(String tableName, ColumnDB column) {
        String indices = ""
        switch (column.type) {
            case Model:
                Constructor<?> ctor = column.type.getConstructor()
                Model refType = (ctor.newInstance() as Model)
                String joinTable = refType.tableName
                String action = column.annotation ? column.annotation.ondelete().toString() : Column.class.getMethod("ondelete").defaultValue.toString()
                indices = "FOREIGN KEY (${column.name}) " +
                    "REFERENCES ${joinTable}(${getColumnName(refType.pk)}) ON DELETE ${action}"
                break
        }
        return indices
    }
    @Override
    boolean renameTable(final DB db, String from, String to) {
        return set(db, "RENAME TABLE ${from} TO ${to}")
    }
}