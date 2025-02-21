package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import com.intellisrc.db.auto.Relational
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.annotation.Annotation
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.auto.Relational.getColumnName

/**
 * Oracle Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.oracle.params = [:]
 *
 * NOTE: In Oracle a user is practically the same as a schema (a collection of tables,
 * or as it is known as database in other engines). In other words, tables created
 * by that user are allocated under its tablespace. When you drop a user you can
 * also remove all tables associated with it.
 */
@CompileStatic
class Oracle extends JDBCServer implements AutoJDBC {
    String dbname = ""
    String user = "SYSTEM"
    String password = ""
    String hostname = "localhost"
    int port = 1521 // ssl port: 2484
    String driver = "oracle.jdbc.driver.OracleDriver"
    boolean supportsJSON = true

    // Oracle specific parameters:
    // https://docs.oracle.com/cd/E13222_01/wls/docs81/jdbc_drivers/oracle.html#1066413
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.any.get("db.oracle.params", [
            BatchPerformanceWorkaround : false,
            LoginTimeout : 0,
            ConnectionRetryCount : 0,
            ConnectionRetryDelay : 3
        ] + params)
    }

    @Override
    String getConnectionString() {
        return "oracle:thin:@//$hostname:$port/$dbname"
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    boolean supportsReplace = false
    boolean convertToLowerCase = false
    String fieldsQuotation = '"'
    String catalogSearchName = "%"
    @Override
    String getSchemaSearchName() {
        return user.toUpperCase()
    }
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }

    /*
     * Must return:
     *      position, column, type, length, default, notnull, primary
     *
     * JDBC is unable to get this information
     *
     * NOTE: We did not include constraint_type 'C' (NOT NULL) as they are not needed here.
     * NOTE: Returning at.data_default won't work as it is LONG inside Oracle and its really
     *       complicated to get that from the DB.
     *
     * @param table
     * @return
     *
    @Override
    String getInfoQuery(String table) {
        return """
            SELECT 
                at.column_id AS "position",
                LOWER(at.column_name) as "column",
                LOWER(at.data_type) as "type",
                '' as "default",
            CASE at.nullable
                WHEN 'N' THEN 0
                WHEN 'Y' THEN 1
            END AS "nullable",
            CASE
                WHEN cc.constraint_type = 'P' THEN 1
                ELSE 0
            END AS "autoinc",
            CASE
                WHEN cc.constraint_type = 'P' THEN 1
                ELSE 0
            END AS "primary",
            CASE
                WHEN cc.constraint_type = 'U' THEN 1
                ELSE 0
            END AS "unique"
            FROM all_tab_columns at
            LEFT JOIN all_cons_columns ac 
              ON (at.owner = ac.owner
                AND at.table_name = ac.table_name 
                AND at.column_name = ac.column_name)
            LEFT JOIN all_constraints cc 
              ON (ac.constraint_name = cc.constraint_name)    
            WHERE cc.constraint_type != 'C' AND LOWER(at.table_name) = LOWER('${table}')"""
    }*/

    /**
     * FIXME: JDBC is unable to get last ID (returning some sequence string id instead, like: 'AAATPDAAHAAAALDAAB')
     *   https://community.oracle.com/tech/developers/discussion/338591/why-does-getgeneratedkeys-return-a-rowid-with-no-numeric-value
     *
     * NOTE: this method is not concurrent safe, if an insert happens between the last
     *       insert and this method, it will be incorrect (it is used as last resort)
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table, String pk) {
        table = table.replace(fieldsQuotation, "")
        return "SELECT ${table}_seq.currval lastId FROM dual"
    }
    /**
     * This will create a schema (user)
     * FIXME: Probably will fail due to permissions
     * @return
     */
    @Override
    String getCreateDatabaseQuery() {
        return "CREATE USER $user IDENTIFIED BY $password; GRANT ALL PRIVILEGES TO $user"
    }
    /**
     * This will remove the user with all its tables (remember that user is a schema)
     * @return
     */
    @Override
    String getDropDatabaseQuery() {
        return "DROP USER $user CASCADE"
    }

        /*
        DB db = connect()
        boolean exists = db.hasTable(table)
        if(exists) {
            boolean hasAutoInc = db.table(table).info().any { it.autoIncrement }
            if (hasAutoInc) {
                db.set(new Query("DROP SEQUENCE ${table}_seq"))
            }
        }
        db.close()
        return exists ? super.getDropTableQuery(table) : true*/

    @Override
    String getBeforeDropTableQuery(String table) {
        return "DROP SEQUENCE ${table}_seq"
    }
    /*
     * In Oracle setting the columns or table names with double quotes makes it case sensitive, but any name can be used.
     */
    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, Collection<Relational.ColumnDB> columns, Annotation meta) {
        boolean ok
        this.meta = meta
        boolean hasAutoIncrement = columns.any { it.annotation.autoincrement() }
        if(hasAutoIncrement) {
            String seqSQL = "CREATE SEQUENCE ${tableName}_seq"
            db.set(new Query(seqSQL))
        }
        String createSQL = "CREATE TABLE ${tableName} (\n"
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
                    type = type.replaceAll("TABLE_NAME", tableName) //Only applies to Oracle
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
                    keys << "KEY ${tableName}_${column.name}_key_index (\"${column.name}\")".toString()
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
                defs << "UNIQUE KEY ${tableName}_${it.key} (\"${it.value.join('\", \"')}\")".toString()
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
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = supportsBoolean ? "BOOLEAN" : "CHAR(6)" //FIXME: in Oracle 23c+ finally it is implemented
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
                type = "NUMBER"
            case short:
                type = type ?: "NUMBER(5)"
            case int:
            case Integer:
            case Model: //Another Model
                type = type ?: "NUMBER(10)"
            case long:
            case Long:
                type = type ?: "NUMBER(19)"
            case BigInteger:
                type = type ?: "NUMBER"
                int len = column.annotation.length()
                String length = len ? "(${len})" : ""
                List<String> extra = [type, length]
                String autoInc = column.annotation.autoincrement() ? " DEFAULT TABLE_NAME_seq.nextval" : "" //TABLE_NAME will be replaced
                extra << (! column.multipleKey && column.annotation.primary() ? "${autoInc} PRIMARY KEY".toString() : "")
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
                boolean isIndex = column.annotation.key() || column.annotation.unique()
                boolean isShort = (column.annotation.length() ?: 256) <= 255
                String varChar = "VARCHAR(${column.annotation.length() ?: 255})"
                type = (isIndex || isShort) ? varChar : "NCLOB"
                break
            case Collection:
            case Map:
                boolean isIndex = column.annotation.key() || column.annotation.unique()
                boolean isShort = (column.annotation.length() ?: 256) <= 255
                boolean json = supportsJSON && meta.hasProperty("useJson") && meta.class.getMethod("useJson").invoke(meta)
                String varChar = "VARCHAR(${column.annotation.length() ?: 255})"
                type = isIndex ? varChar : (json ? "JSON" : (isShort ? varChar : "NCLOB"))
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
                    type = len < 256 ? "VARCHAR($len)" : "NCLOB"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.v("If you want to able to use '%s' type in the database, either set `fromString` " +
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
                String onDelete = switch (action.toLowerCase()) {
                    case "restrict" -> ""
                    default -> "ON DELETE ${action}"
                }

                indices = "CONSTRAINT fk_${column.name} FOREIGN KEY (\"${column.name}\") " +
                    "REFERENCES ${joinTable}(\"${getColumnName(refType.pk)}\") ${onDelete}"
                break
        }
        return indices
    }
}
