//file:noinspection GetterMethodCouldBeProperty
package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.DB
import com.intellisrc.db.Database
import com.intellisrc.db.TableDefinition
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
class SQLite extends JDBC implements AutoJDBC {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = "org.sqlite.JDBC"
    String tableMeta = Config.any.get("db.sqlite.meta", "_meta")
    boolean fkEnabled = Config.any.get("db.sqlite.fk", true) // ON By default
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
        return "SELECT name FROM sqlite_schema WHERE type='table' AND name='${tableName}'"
    }
    @Override
    boolean initialize() {
        boolean ok = true
        if(memory) {
            /*
                The reason is because we need to use a single connection
                everywhere and the _meta table gets destroyed after db.close().
                Although it is possible to make it work using an static connection
                it is better to use H2 in Memory (it works fine) instead.
             */
            Log.w("Auto functionality does not work in Memory")
            return false
        }
        //Be sure we start a new connection:
        DB db = new Database(this).connect()
        if(! db.table(tableMeta).exists()) {
            TableDefinition tableDefinition = new TableDefinition(version: 0) // 0 == Do not set
            tableDefinition.addAll([
                new ColumnDefinition(
                    name: "table_name",
                    type: String,
                    primaryKey: true,
                    nullable: false,
                    length: 255
                ),
                new ColumnDefinition(
                    name: "version",
                    type: Integer,
                    nullable: false,
                    defaultValue: 1
                )])
            ok = db.table(tableMeta).createTable(tableDefinition)
        }
        db.close()
        return ok
    }

    @Override
    String getCreateTableSQL(String tableName, TableDefinition definitions) {
        List<String> defs = []
        List<ColumnDefinition> pks = definitions.pks
        boolean isMultiplePks = definitions.hasMultiplePk()

        definitions.each {
            ColumnDefinition col ->
                // SQLite Requirement: Auto-increment MUST be defined on an 'INTEGER' type inline
                String typeDef = col.autoIncrement && !isMultiplePks ? "INTEGER" : getColumnDefinitionCustom(col)

                List<String> parts = ["`${col.name}`".toString(), typeDef]

                if (col.defaultValue) {
                    parts << getDefaultQuery(col)
                } else if (!col.nullable &&! col.primaryKey) {
                    parts << "NOT NULL"
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
        if (definitions.engine) {
            Log.w("SQLite doesn't support engines (trying to set: %s)", definitions.engine)
        }
        return ("CREATE TABLE IF NOT EXISTS `${tableName}` (\n" + defs.join(",\n") + "\n)").toString()
    }

    @Override
    String getAutoIncrementBindSQL() {
        return "AUTOINCREMENT"
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
        //String qry = "SELECT sql FROM sqlite_master WHERE type='table' AND name='${from}'".toString()
        //return qry.replaceAll(/CREATE TABLE `?${from}`?/, "CREATE TABLE `${to}`")
        return "CREATE TABLE `${to}` AS SELECT * FROM `${from}` WHERE 1=0"
    }
    @Override
    String getVersionUpdate(String table, int version) {
        return "REPLACE INTO ${tableMeta} (table_name, version) VALUES('${table}',${version})".toString()
    }
    @Override
    String getVersionRead(String table) {
        return "SELECT version FROM ${tableMeta} WHERE table_name = '${table}' LIMIT 1".toString()
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
    String getIdentitySQL(String table, String columnName) {
        return "SELECT seq FROM sqlite_sequence WHERE name = '${table}'"
    }

    @Override
    String getIdentityUpdateSQL(String table, String columnName, int value) {
        return value <= 1 ? "DELETE FROM sqlite_sequence WHERE name = '${table}'" :
              "UPDATE sqlite_sequence SET seq = ${value} WHERE name = '${table}'"
    }
}
