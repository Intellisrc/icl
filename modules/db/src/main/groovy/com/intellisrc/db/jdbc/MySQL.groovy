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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.regex.Matcher

import static com.intellisrc.db.auto.Table.ColumnDB
import static com.intellisrc.db.auto.Table.getColumnName

/**
 * MySQL Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.mysql.params = [:]
 */
@CompileStatic
class MySQL extends JDBCServer implements AutoJDBC {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = 3306
    String driver = "com.mysql.cj.jdbc.Driver"
    // Most common parameters:
    boolean compression = false
    boolean ssl = false
    boolean trustCert = true

    // MySQL Parameters
    // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html
    // https://mysql-net.github.io/MySqlConnector/connection-options/
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.mysql.params", [
            allowMultiQueries       : false,
            connectTimeout          : 0,
            socketTimeout           : 0,
            useCompression          : compression,
            useSSL                  : ssl,
            verifyServerCertificate : ! trustCert,
            autoReconnect           : true,
            //UTF-8 enable:
            useUnicode              : true,
            characterEncoding       : "UTF-8",
            characterSetResults     : "utf8",
            connectionCollation     : "utf8_general_ci",
            allowPublicKeyRetrieval : ! ssl     //

            // These properties are not compatible with MariaDB:
            //emptyStringsConvertToZero : true,
            //paranoid                : false,
            //requireSSL              : false,
            //useTimezone             : false,
            //useUnicode              : true,
        ] + params)
    }

    @Override
    String getConnectionString() {
        String proto = this.toString() // mysql or mariadb
        return "$proto://$hostname:$port/$dbname?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String fieldsQuotation = '`'
    String tablesQuotation = '`'
    boolean useFetch = false

    /**
     * Fallback Query to get last ID
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table, String pk) {
        return "SELECT LAST_INSERT_ID() as lastid"
    }

    /////////////////////////////// AUTO ////////////////////////
    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, Collection<ColumnDB> columns) {
        boolean ok
        String createSQL = "CREATE TABLE IF NOT EXISTS `${tableName}` (\n"
        List<String> defs = []
        List<String> keys = []
        Map<String, List<String>> uniqueGroups = [:]
        List<ColumnDB> pks = columns.findAll { it.annotation.primary() }.toList()
        if(pks.size() > 1) {
            pks.each { it.multipleKey = true }
        }
        columns.each {
            ColumnDB column ->
                List<String> parts = ["`${column.name}`".toString()]
                if (column.annotation.columnDefinition()) {
                    parts << column.annotation.columnDefinition()
                } else {
                    String type = getColumnDefinition(column)
                    parts << type

                    if (column.defaultVal) {
                        parts << getDefaultQuery(column)
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
        if (pks.size() > 1) {
            defs << ("PRIMARY KEY (" + (pks.collect {"`${ it.name }`" }).join(",") + ")")
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
    boolean turnFK(final DB db, boolean on) {
        return set(db, String.format("SET FOREIGN_KEY_CHECKS=%d", on ? 1 : 0))
    }
    @Override
    boolean copyTableStructure(final DB db, String from, String to) {
        return set(db, "CREATE TABLE $to LIKE $from") && copyTableData(db, from, to, [])
    }
    @Override
    boolean copyTableData(final DB db, String from, String to, Collection<ColumnDB> columns) {
        return set(db, "INSERT IGNORE INTO $to SELECT * FROM $from")
    }
    @Override
    boolean setVersion(final DB db, String dbname, String table, int version) {
        return set(db, "ALTER TABLE ${table} COMMENT = 'v.${version}'")
    }
    @Override
    int getVersion(final DB db, String dbname, String table) {
        String verStr = get(db, "SELECT table_comment FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='${dbname}' AND table_name='${table}'").toString()
        int version = 1
        if(verStr) {
            Matcher matcher = (verStr =~ /(\d+)/)
            if(matcher.find()) {
                version = matcher.group(1) as int
            }
        }
        return version
    }

    /**
     * Return SQL column definition for a field
     * @param field
     * @param column
     * @return
     */
    @Override
    String getColumnDefinition(ColumnDB column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = "ENUM('true','false')"
                break
            case char:
            case Character:
                type = "CHAR"
                break
            case char[]:
                int len = column.annotation.length()
                if(!len) {
                    Log.w("Column: %s is char array but has no length. Setting 2 as length.", column.name)
                    len = 2
                }
                type = "CHAR($len)"
                break
            case String:
                type = "VARCHAR(${column.annotation.length() ?: 255})"
                break
            // All numeric values share unsigned/autoincrement and primary instructions:
            case byte:
                type = type ?: "TINYINT" //no break
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
                extra << (column.annotation.unsigned() ? "UNSIGNED" : "")
                extra << (column.annotation.primary() && column.annotation.autoincrement() ? "AUTO_INCREMENT" : "")
                extra << (! column.multipleKey && column.annotation.primary() ? "PRIMARY KEY" : "")
                type = extra.findAll {it }.join(" ")
                break
            case float:
            case Float:
                type = "FLOAT"
                break
            case double:
            case Double:
            case BigDecimal:
                type = "DOUBLE"
                break
            case LocalDate:
                type = "DATE"
                break
            case LocalDateTime:
                type = "DATETIME"
                break
            case LocalTime:
                type = "TIME"
                break
            case Inet4Address:
                type = "VARCHAR(${column.annotation.length() ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column.annotation.length() ?: 45})"
                break
            case URL:
            case URI:
            case Collection:
            case Map:
                type = column.annotation.key() || column.annotation.unique() || (column.annotation.length() ?: 256) <= 255 ? "VARCHAR(${column.annotation.length() ?: 255})" : "TEXT"
                break
            case Enum:
                type = "ENUM('" + column.type.getEnumConstants().join("','") + "')"
                break
            case byte[]:
                int len = column.annotation.length() ?: 65535
                switch (true) {
                    case len < 256      : type = "TINYBLOB"; break
                    case len < 65536    : type = "BLOB"; break
                    case len < 16777216 : type = "MEDIUMBLOB"; break
                    default             : type = "LONGBLOB"; break
                }
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
                    type = len < 256 ? "VARCHAR($len)" : "TEXT"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.d("If you want to able to use '%s' type in the database, either set `fromString` " +
                        "as static method or set a constructor which accepts `String`", column.type.simpleName)
                }
        }
        return type
    }

    /**
     * Get FK
     * @param field
     * @return
     */
    @Override
    String getForeignKey(String tableName, final ColumnDB column) {
        String indices = ""
        switch (column.type) {
            case Model:
                Constructor<?> ctor = column.type.getConstructor()
                Model refType = (ctor.newInstance() as Model)
                String joinTable = refType.tableName
                String action = column.annotation ? column.annotation.ondelete().toString() : Column.class.getMethod("ondelete").defaultValue.toString()
                indices = "FOREIGN KEY (`${column.name}`) " +
                    "REFERENCES `${joinTable}`(`${getColumnName(refType.pk)}`) ON DELETE ${action}"
                break
        }
        return indices
    }
}
