package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.DB
import com.intellisrc.db.TableDefinition
import com.intellisrc.db.Volatile
import com.intellisrc.db.annot.UpdateActions
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.ENUM

/**
 * SQLite Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.sqlite.memory = false
 */
@CompileStatic
class SQLite extends JDBC implements AutoJDBC, Volatile {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = "org.sqlite.JDBC"
    String tableMeta = Config.any.get("db.sqlite.meta", "_meta")
    boolean fkEnabled = Config.any.get("db.sqlite.fk", true) // ON By default
    boolean useVersion = false
    BooleanHandle booleanHandle = ENUM

    // SQLite specific parameters:
    boolean memory = Config.any.get("db.sqlite.memory", false)

    @Override
    String getConnectionString() {
        return  connectionURI ?: "sqlite:" + (memory ? ":memory:" : dbname) + (parameters.isEmpty() ? "" : "?" + parameters.toQueryString())
    }

    @Override
    protected Map getParameters() {
        return Config.any.get("db.sqlite.params", [
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
    String getTableExistsSQL(String tableName) {
        return "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='${tableName.toUpperCase()}'"
    }
    @Override
    void initialize(DB db) {
        if(useVersion &&! db.table(tableMeta).exists()) {
            db.table(tableMeta).createTable([
                new ColumnDefinition(
                    name: "table_name",
                    type: String,
                    index: true,
                    nullable: false,
                    length: 255
                ),
                new ColumnDefinition(
                    name: "version",
                    type: Integer,
                    nullable: false,
                    defaultValue: 1
                ),
            ] as TableDefinition)
        }
    }

    @Override
    String getCreateTableSQL(String tableName, TableDefinition definitions = [] as TableDefinition, String charset = "", String engine = "", int version = 1) {
        List<String> defs = []
        List<ColumnDefinition> pks = definitions.pks
        boolean isMultiplePks = definitions.hasMultiplePk()

        definitions.each {
            ColumnDefinition col ->
                // SQLite Requirement: Auto-increment MUST be defined on an 'INTEGER' type inline
                String typeDef = col.autoIncrement && !isMultiplePks ? "INTEGER" : getColumnDefinitionCustom(col)

                List<String> parts = ["`${col.name}`".toString(), typeDef]

                if (!col.nullable &&! col.primaryKey) {
                    parts << "NOT NULL"
                }

                if (col.defaultValue) {
                    parts << getDefaultQuery(col)
                }

                if(col.primaryKey) {
                    if(! isMultiplePks) {
                        parts << "PRIMARY KEY"
                        if(col.autoIncrement) {
                            parts << "AUTOINCREMENT"
                        }
                    }
                }

                if (col.unique &&! col.uniqueGroup) {
                    parts << "UNIQUE"
                }

                defs << parts.join(' ')
        }

        if (isMultiplePks) {
            defs << ("PRIMARY KEY (" + (pks.collect {"`${ it.name }`" }).join(",") + ")")
        }

        // Append Composite Unique Groups
        Map<String, List<String>> uniqueGroups = definitions.uniqueGroups
        if (!uniqueGroups.empty) {
            uniqueGroups.each { String groupName, List<String> columns ->
                defs << "UNIQUE (" + columns.collect { "`${it}`" }.join(",") + ")"
            }
        }

        // Append Foreign Keys
        String fks = definitions.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }
        if (engine) {
            Log.w("SQLite doesn't support engines (trying to set: %s)", engine)
        }
        return ("CREATE TABLE IF NOT EXISTS `${tableName}` (\n" + defs.join(",\n") + "\n)").toString()
    }

    @Override
    List<String> getUpdateIndicesSQL(String tableName, List<String> columns) {
        return columns.collect {
            ("CREATE INDEX IF NOT EXISTS `${tableName}_${it}_index` ON `${tableName}` (`${it}`)").toString()
        }
    }

    @Override
    String getTurnFK(boolean on) {
        return String.format("PRAGMA foreign_keys = %s", on ? "ON" : "OFF")
    }
    @Override
    String getCopyTableStructureSQL(String from, String to, TableDefinition columns = [] as TableDefinition) {
        String qry = "SELECT sql FROM sqlite_master WHERE type='table' AND name='${from}'".toString()
        return qry.replaceAll(/CREATE TABLE `?${from}`?/, "CREATE TABLE `${to}`")
    }
    @Override
    String getVersionUpdate(String table, int version) {
        useVersion = true
        return "REPLACE INTO ${tableMeta} (table_name, version) VALUES(${table},${version})".toString()
    }
    @Override
    String getVersionRead(String table) {
        return useVersion ?
            "SELECT version FROM ${tableMeta} WHERE table_name = ${table} LIMIT 1".toString() : ""
    }

    @Override
    String getColumnDefinition(final ColumnDefinition column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
            case char:
            case Character:
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
            case Byte:
            case short:
            case Short:
            case int:
            case Integer:
            case BigInteger:
            case long:
            case Long:
            case Model: //Another Model
                type = "INTEGER"
                List<String> extra = [type]
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
                    Log.v("If you want to able to use '%s' type in the database, either set `fromString` " +
                        "as static method or set a constructor which accepts `String`", column.type.simpleName)
                }
        }
        return type
    }

    @Override
    String getAutoIncrementSQL(String table, String columnName) {
        return "SELECT seq FROM sqlite_sequence WHERE name = '${table}'"
    }

    @Override
    String getAutoIncrementUpdateSQL(String table, String columnName, long value) {
        return "INSERT INTO sqlite_sequence (name, seq) VALUES ('${table}', ${value}) " +
                "ON CONFLICT(name) DO UPDATE SET seq = excluded.seq"
    }

    @Override
    String getForeignKey(String tableName, ColumnDefinition column) {
        if (!column.isForeignKey) return ""
        String sql = "FOREIGN KEY (`${column.name}`) REFERENCES `${column.referenceTable}`(`${column.referenceColumn}`) ON DELETE ${column.onDelete}"
        if (column.onUpdate && column.onUpdate != UpdateActions.NO_ACTION) {
            sql += " ON UPDATE ${column.onUpdate}"
        }
        return sql
    }
}
