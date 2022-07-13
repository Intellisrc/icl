package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.annot.Column
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import com.intellisrc.db.auto.Table.ColumnDB
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.auto.Table.getColumnName

/**
 * SQLite Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.sqlite.memory = false
 */
@CompileStatic
class SQLite extends JDBC implements AutoJDBC {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = "org.sqlite.JDBC"
    String tableMeta = Config.get("db.sqlite.meta", "_meta")
    boolean fkEnabled = Config.get("db.sqlite.fk", true) // ON By default

    // SQLite specific parameters:
    boolean memory = Config.get("db.sqlite.memory", false)
    @Override
    String getConnectionString() {
        return (memory ? "sqlite::memory:" : "sqlite:$dbname") + (parameters.isEmpty() ? "" : "?" + parameters.toQueryString())
    }

    @Override
    protected Map getParameters() {
        return Config.get("db.sqlite.params", [
            foreign_keys : fkEnabled
        ])
    }

    @Override
    String getCatalogSearchName() {
        return "%"
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String fieldsQuotation = '`'
    boolean supportsDate = false
    boolean useFetch = false

    @Override
    String getCreateDatabaseQuery() {
        return "" // Not needed
    }

    @Override
    String getDropDatabaseQuery() {
        return "" // Not needed
    }

    /**
     * Truncate a table
     * ... when you execute a DELETE statement without a WHERE clause, the TRUNCATE optimizer
     * is run instead of the normal delete behavior...
     * @param table
     * @return
     */
    @Override
    String getTruncateQuery(String table) {
        return "DELETE FROM $table"
    }

    //////////////////////// AUTO ////////////////////////////
    @Override
    void autoInit(DB db) {
        db.set(new Query("CREATE TABLE IF NOT EXISTS `$tableMeta` (" +
            "table_name TEXT KEY NOT NULL UNIQUE," +
            "version INTEGER NOT NULL DEFAULT 1" +
        ")"))
    }

    @Override
    boolean createTable(DB db, String tableName, String charset, String engine, int version, List<ColumnDB> columns) {
        boolean ok
        String createSQL = "CREATE TABLE IF NOT EXISTS `${tableName}` (\n"
        List<String> defs = []
        List<String> keys = []
        Map<String, List<String>> uniqueGroups = [:]
        columns.each {
            ColumnDB column ->
                List<String> parts = ["`${column.name}`".toString()]
                if (column.annotation.columnDefinition()) {
                    parts << column.annotation.columnDefinition()
                } else {
                    String type = getColumnDefinition(column) +
                                  (column.annotation.key() ? " KEY" : "")
                    parts << type

                    if (column.defaultVal) {
                        parts << getDefaultQuery(column, true)
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
                defs << parts.join(' ')
        }
        if (!keys.empty) {
            defs.addAll(keys)
        }
        String fks = columns.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }
        if (engine) {
            Log.w("SQLite doesn't support engines (trying to set: %s)", engine)
        }
        createSQL += defs.join(",\n") + "\n)"
        ok = db.set(new Query(createSQL))
        if(! ok) {
            Log.v(createSQL)
            Log.e("Unable to create table.")
        }

        return ok
    }

    @Override
    boolean turnFK(final DB db, boolean on) {
        return set(db, String.format("PRAGMA foreign_keys = %s", on ? "ON" : "OFF"))
    }
    @Override
    boolean copyTableDesc(final DB db, String from, String to) {
        String qry = get(db, "SELECT sql FROM sqlite_master WHERE type='table' AND name='${from}'").toString()
        return set(db, qry.replace("CREATE TABLE ${from}", "CREATE TABLE ${to}"))
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
            case String:
            case Inet4Address:
            case Inet6Address:
            case InetAddress:
            case LocalDate:
            case LocalDateTime:
            case LocalTime:
            case URL:
            case URI:
            case Collection:
            case Map:
            case Enum:
                type = "TEXT"
                break
            case byte:
            case short:
            case int:
            case Integer:
            case BigInteger:
            case long:
            case Long:
            case Model: //Another Model
                type = "INTEGER"
                List<String> extra = [type]
                extra << (column.annotation.primary() ? "PRIMARY KEY" : "")
                extra << (column.annotation.primary() && column.annotation.autoincrement() ? "AUTOINCREMENT" : "") //Autoincrement is after Primary Key
                type = extra.findAll {it }.join(" ")
                break
            case float:
            case Float:
            case double:
            case Double:
            case BigDecimal:
                type = "FLOAT"
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
                    type = "TEXT"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.d("If you want to able to use '%s' type in the database, either set `fromString` " +
                        "as static method or set a constructor which accepts `String`", column.type.simpleName)
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
                indices = "FOREIGN KEY (`${column.name}`) " +
                    "REFERENCES `${joinTable}`(`${getColumnName(refType.pk)}`) ON DELETE ${action}"
                break
        }
        return indices
    }
}
