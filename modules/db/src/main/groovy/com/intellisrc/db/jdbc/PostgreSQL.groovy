package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import com.intellisrc.db.auto.Relational
import com.intellisrc.db.auto.Table
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.regex.Matcher

import static com.intellisrc.db.auto.Relational.getColumnName

/**
 * PostgreSQL Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.postgresql.params = [:]
 *
 */
@CompileStatic
class PostgreSQL extends JDBCServer implements AutoJDBC {
    String dbname = ""
    String user = "postgres"
    String password = "password"
    String hostname = "localhost"
    int port = 5432
    String driver = "org.postgresql.Driver"
    // Most common:
    boolean readOnly = false
    boolean ssl = false
    // PostgreSQL specific parameters:
    // https://jdbc.postgresql.org/documentation/head/connect.html
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.postgresql.params", [
            binaryTransfer      : true,
            cleanupSavepoints   : false,
            connectTimeout      : 0,
            loginTimeout        : 0,
            readOnly            : readOnly,
            socketTimeout       : 0,
            ssl                 : ssl,
            tcpKeepAlive        : false,
        ] + params)
    }
    @Override
    String getConnectionString() {
        return "postgresql://$hostname:$port/$dbname" + (parameters.ssl ? "?ssl=true" : "")
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    boolean useFetch = false
    boolean supportsReplace = false
    boolean supportsBoolean = true
    /**
     * Fallback for last ID
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table, String pk) {
        return "SELECT lastval()"
    }
    String getTruncateQuery(String table) {
        return "TRUNCATE TABLE \"$table\" CASCADE"
    }
    String getDropTableQuery(String table) {
        return "DROP TABLE \"$table\" CASCADE"
    }

    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, Collection<Relational.ColumnDB> columns) {
        boolean ok
        String createSQL = "CREATE TABLE IF NOT EXISTS \"${tableName}\" (\n"
        List<String> defs = []
        List<String> keys = []
        Map<String, List<String>> uniqueGroups = [:]
        List<Relational.ColumnDB> pks = columns.findAll { it.annotation.primary() }.toList()
        if(pks.size() > 1) {
            pks.each { it.multipleKey = true }
        }
        columns.each {
            Relational.ColumnDB column ->
                List<String> parts = ["\"${column.name}\"".toString()]
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
                    keys << "KEY \"${tableName}_${column.name}_key_index\" (\"${column.name}\")".toString()
                }
                defs << parts.join(' ')
        }
        if (!keys.empty) {
            defs.addAll(keys)
        }
        if (pks.size() > 1) {
            defs << ("PRIMARY KEY (" + (pks.collect {"\"${ it.name }\"" }).join(",") + ")")
        }
        if (!uniqueGroups.keySet().empty) {
            uniqueGroups.each {
                defs << "UNIQUE KEY \"${tableName}_${it.key}\" (\"${it.value.join('\", \"')}\")".toString()
            }
        }
        String fks = columns.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }
        createSQL += defs.join(",\n") + "\n)"
        ok = db.set(new Query(createSQL))
        if(ok) {
            db.set(new Query("COMMENT ON TABLE ${tableName} IS 'v.${version}'"))
        } else {
            Log.v(createSQL)
            Log.e("Unable to create table.")
        }
        return ok
    }

    @Override
    String getColumnDefinition(Relational.ColumnDB column) {
        String type = ""
        boolean serial = column.annotation.autoincrement()
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = "BOOLEAN"
                break
            case char:
            case Character:
                type = "CHARACTER"
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
                type = serial ? "SMALLSERIAL" : "TINYINT" //no break
            case short:
                type = type ?: (serial ? "SMALLSERIAL" : "SMALLINT")
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: (serial ? "SERIAL" : "INTEGER")
            case BigInteger:
            case long:
            case Long:
                type = type ?: (serial ? "BIGSERIAL" : "BIGINT")
                int len = column.annotation.length()
                String length = len ? "(${len})" : ""
                List<String> extra = [type, length]
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

    @Override
    String getForeignKey(String tableName, Relational.ColumnDB column) {
        String indices = ""
        switch (column.type) {
            case Model:
                Constructor<?> ctor = column.type.getConstructor()
                Model refType = (ctor.newInstance() as Model)
                String joinTable = refType.tableName
                String action = column.annotation ? column.annotation.ondelete().toString() : Column.class.getMethod("ondelete").defaultValue.toString()
                indices = "FOREIGN KEY (\"${column.name}\") " +
                    "REFERENCES \"${joinTable}\"(\"${getColumnName(refType.pk)}\") ON DELETE ${action}"
                break
        }
        return indices
    }

    @Override
    boolean turnFK(final DB db, boolean on) {
        return set(db, String.format("SET session_replication_role = '%s'", on ? "origin" : "replica"))
    }
    @Override
    boolean copyTableStructure(final DB db, String from, String to) {
        return false //set(db, "CREATE TABLE \"$to\" (LIKE \"$from\" INCLUDING ALL)")
    }
    @Override
    boolean copyTableData(final DB db, String from, String to, Collection<Relational.ColumnDB> columns) {
        return set(db, "INSERT INTO \"${to}\" (SELECT * FROM ${from})")
    }
    @Override
    boolean resetAutoIncrement(DB db, Table newTable, String name, String backup) {
        String field = newTable.autoIncrement
        return field ? get(db, "SELECT setval(pg_get_serial_sequence('${name}','${field}'), nextval(pg_get_serial_sequence('${backup}','${field}')), FALSE)").toInt() > 0 : true
    }
    @Override
    boolean setVersion(final DB db, String dbname, String table, int version) {
        return set(db, "COMMENT ON TABLE \"${table}\" IS 'v.${version}'")
    }
    @Override
    int getVersion(final DB db, String dbname, String table) {
        String verStr = get(db, "SELECT obj_description('public.${table}'::regclass)").toString()
        int version = 1
        if(verStr) {
            Matcher matcher = (verStr =~ /(\d+)/)
            if(matcher.find()) {
                version = matcher.group(1) as int
            }
        }
        return version
    }
}
