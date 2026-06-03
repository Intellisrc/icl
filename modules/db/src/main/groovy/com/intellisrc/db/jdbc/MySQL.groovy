//file:noinspection GetterMethodCouldBeProperty
package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.DB
import com.intellisrc.db.TableDefinition
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.ColumnDefinition.UNLIMITED
import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.*

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
    boolean supportsJSON = true
    BooleanHandle booleanHandle = ENUM

    // MySQL Parameters
    // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html
    // https://mysql-net.github.io/MySqlConnector/connection-options/
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.any.get("db.mysql.params", [
            allowMultiQueries       : false,
            connectTimeout          : DB.connectionTimeout * Millis.SECOND,
            socketTimeout           : 0,
            useCompression          : compression,
            useSSL                  : ssl,
            verifyServerCertificate : ! trustCert,
            //UTF-8 enable:
            useUnicode              : true,
            characterEncoding       : "UTF-8",
            characterSetResults     : "utf8",
            connectionCollation     : "utf8_general_ci",
            allowPublicKeyRetrieval : ! ssl

            // https://dev.mysql.com/doc/c-api/8.4/en/c-api-auto-reconnect.html
            // autoReconnect           : false, //Not recommended (deprecated)

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
        return connectionURI ?: "$proto://$hostname:$port/$dbname?" + parameters.toQueryString()
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

    ////////////// SHARED (FLUID & AUTO) /////////////////////
    /**
     * Create table using TableColumnDefinition
     * @param db
     * @param tableName
     * @param charset
     * @param engine
     * @param version
     * @param definitions
     * @return
     */
    @Override
    String getCreateTableSQL(String tableName, TableDefinition definitions) {
        List<String> defs = []
        List<String> keys = []
        List<ColumnDefinition> pks = definitions.pks
        boolean isMultiplePks = definitions.hasMultiplePk()
        int version = definitions.version

        // Process columns
        definitions.each {
            ColumnDefinition col ->
                List<String> parts = ["`${col.name}`".toString(), getColumnDefinitionCustom(col)]

                if (col.defaultValue) {
                    parts << getDefaultQuery(col)
                } else if (!col.nullable &&! col.primaryKey) {
                    parts << "NOT NULL"
                }

                if(col.autoIncrement) {
                    parts << "AUTO_INCREMENT"
                }

                if(col.primaryKey) {
                    if(! isMultiplePks) {
                        parts << "PRIMARY KEY"
                    }
                }

                if (col.unique &&! col.uniqueGroup) {
                    parts << "UNIQUE"
                }
                defs << parts.join(' ')

                if (col.index) {
                    keys << "KEY `${tableName}_${col.name}_key_index` (`${col.name}`)".toString()
                }
        }

        // Append secondary keys
        if (!keys.empty) {
            defs.addAll(keys)
        }

        // Append Composite Primary Key Constraints (if any)
        if (isMultiplePks) {
            defs << "PRIMARY KEY (" + pks.collect { "`${it.name}`" }.join(",") + ")"
        }

        // Append Composite Unique Group Constraints
        Map<String, List<String>> uniqueGroups = definitions.uniqueGroups
        if (!uniqueGroups.isEmpty()) {
            uniqueGroups.each { String groupName, List<String> columns ->
                defs << "UNIQUE KEY `${tableName}_${groupName}` (`${columns.join('`, `')}`)".toString()
            }
        }

        // Append Foreign Keys
        String fks = definitions.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) defs << fks

        // Assemble Final Statement
        String enginePart = definitions.engine ? "ENGINE=${definitions.engine} " : ""
        String createSQL = "CREATE TABLE IF NOT EXISTS `${tableName}` (\n" +
            defs.join(",\n") +
            "\n) ${enginePart}CHARACTER SET=${definitions.charset}\nCOMMENT='v.${version}'"

        return createSQL
    }

    @Override
    String getAutoIncrementBindSQL() {
        return "AUTO_INCREMENT"
    }

    @Override
    String getTurnFK(boolean on) {
        return String.format("SET FOREIGN_KEY_CHECKS=%d", on ? 1 : 0)
    }

    @Override
    String getCopyTableStructureSQL(String from, String to, TableDefinition columns) {
        return "CREATE TABLE `${to}` LIKE `${from}`"
    }

    @Override
    String getVersionUpdate(String table, int version) {
        return "ALTER TABLE ${table} COMMENT = 'v.${version}'"
    }

    @Override
    String getVersionRead(String table) {
        return "SELECT table_comment FROM INFORMATION_SCHEMA.TABLES WHERE table_schema='${dbname}' AND table_name='${table}'"
    }

    @Override
    String getIdentitySQL(String table, String columnName) {
        return "SELECT (AUTO_INCREMENT - 1) FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = '${dbname}' AND TABLE_NAME = '${table}'"
    }

    @Override
    String getIdentityUpdateSQL(String table, String columnName, int value) {
        return "ALTER TABLE ${table} AUTO_INCREMENT = ${value + 1}"
    }

    /**
     * Return SQL column definition for a field
     * @param field
     * @param column
     * @return
     */
    @Override
    String getColumnDefinition(ColumnDefinition column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = switch (booleanHandle) {
                    case BOOLEAN -> "BOOLEAN"
                    case NUMBER -> "TINYINT(1)"
                    case CHAR -> "CHAR"
                    case ENUM -> "ENUM('TRUE','FALSE')"
                }
                break
            case char:
            case Character:
                type = "CHAR"
                break
            case char[]:
                int len = column.length
                if(!len) {
                    Log.w("Column: %s is char array but has no length. Setting 2 as length.", column.name)
                    len = 2
                }
                type = "CHAR($len)"
                break
            case String:
                type = column.length == UNLIMITED ? "TEXT" : "VARCHAR(${column.length ?: 255})"
                break
            // All numeric values share unsigned/autoincrement and primary instructions:
            case byte:
            case Byte:
                type = type ?: "TINYINT" //no break
                break
            case short:
            case Short:
                type = type ?: "SMALLINT"
                break
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: "INT"
                break
            case BigInteger:
            case long:
            case Long:
                type = type ?: "BIGINT"
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
                type = "VARCHAR(${column.length ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column.length ?: 45})"
                break
            case URL:
            case URI:
                boolean isUrlShort = (column.length ?: 256) <= 255
                type = (column.index || column.unique || isUrlShort) ? "VARCHAR(${column.length ?: 255})" : "TEXT"
                break
            case Collection:
            case Map:
                boolean isCollShort = (column.length ?: 256) <= 255
                boolean json = supportsJSON && meta && meta.hasProperty("useJson") && meta.class.getMethod("useJson").invoke(meta)
                type = (column.index || column.unique) ? "VARCHAR(${column.length ?: 255})" : (json ? "JSON" : (isCollShort ? "VARCHAR(${column.length ?: 255})" : "TEXT"))
                break
            case Enum:
                type = "ENUM('" + column.type.getEnumConstants().join("','") + "')"
                break
            case byte[]:
                int len = column.length ?: 65535
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
                    int len = column.length ?: 256
                    type = len < 256 ? "VARCHAR($len)" : "TEXT"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.v("If you want to able to use '%s' type in the database, either set `fromString` " +
                        "as static method or set a constructor which accepts `String`", column.type.simpleName)
                }
        }
        if (column.unsigned) {
            if (type in ["TINYINT", "SMALLINT", "INT", "BIGINT"]) {
                type += " UNSIGNED"
            }
        }
        return type
    }
}
