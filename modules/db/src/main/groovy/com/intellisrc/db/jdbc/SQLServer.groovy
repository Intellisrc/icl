package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.DB
import com.intellisrc.db.TableDefinition
import com.intellisrc.db.annot.UpdateActions
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.ColumnDefinition.UNLIMITED
import static com.intellisrc.db.auto.Relational.getColumnName
import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.NUMBER

/**
 * MS SQL Server
 * @since 2022/01/18.
 *
 * Additional settings:
 * db.sqlserver.params = [:]
 */
@CompileStatic
class SQLServer extends JDBCServer implements AutoJDBC {
    String dbname = ""
    String user = "sa"
    String password = ""
    String hostname = "localhost"
    int port = 1433
    String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    // SQLServer specific parameters:
    boolean useWinLogin = false
    boolean trustCert = false   // In production not recommended to turn 'true'
    boolean supportsJSON = true
    BooleanHandle booleanHandle = NUMBER

    boolean secure = false
    boolean strict = false // Only if secure = true

    @Override
    Map getParameters() {
        return Config.any.get("db.sqlserver.params", [
            encrypt : secure && strict ? "strict" : secure.toString(),
            integratedSecurity : useWinLogin,
            loginTimeout : DB.connectionTimeout, //Seconds
            trustServerCertificate : trustCert,
        ] + params)
    }

    @Override
    String getConnectionString() {
        return connectionURI ?: "jdbc:sqlserver://$hostname:$port;" +
            (dbname ? "database=$dbname;" : "" ) +
            parameters.collect {
                "${it.key}=${it.value}"
            }.join(";")
    }

    // QUERY BUILDING -------------------------
    String fieldsQuotation = '"'
    String tablesQuotation = '"'
    String schemaSearchName = "dbo"
    boolean supportsReplace = false

    @Override
    String getLastIdQuery(String table, String pk) {
        return "SELECT SCOPE_IDENTITY()"
    }

    @Override
    String getLimitQuery(int limit, int offset, boolean hasOrder) {
        return (hasOrder ? "" : "ORDER BY 1 ") + "OFFSET $offset ROWS" + (limit > 0 ? " FETCH NEXT $limit ROWS ONLY" : "")
    }

    // AUTO-DDL & AUTO-JDBC -------------------------

    @Override
    String getCreateTableSQL(String tableName, TableDefinition definitions = [] as TableDefinition, String charset = "", String engine = "") {
        List<String> defs = []
        List<ColumnDefinition> pks = definitions.pks
        boolean isMultiplePks = definitions.hasMultiplePk()

        definitions.each { ColumnDefinition col ->
            List<String> parts = ["\"${col.name}\"".toString(), getColumnDefinitionCustom(col)]

            if (col.autoIncrement && !isMultiplePks) {
                parts << "IDENTITY(1,1)"
            }

            if (!col.nullable && !col.primaryKey) {
                parts << "NOT NULL"
            }

            if (col.defaultValue) {
                parts << getDefaultQuery(col)
            }

            if (col.primaryKey && !isMultiplePks) {
                parts << "PRIMARY KEY"
            }

            if (col.unique && !col.uniqueGroup) {
                parts << "UNIQUE"
            }
            defs << parts.join(' ')
        }

        if (isMultiplePks) {
            defs << "PRIMARY KEY (" + pks.collect { "\"${it.name}\"" }.join(",") + ")"
        }

        // Composite Unique Groups
        Map<String, List<String>> uniqueGroups = definitions.uniqueGroups
        if (!uniqueGroups.isEmpty()) {
            uniqueGroups.each { String groupName, List<String> columns ->
                defs << "CONSTRAINT \"${tableName}_${groupName}\" UNIQUE (${columns.collect { "\"${it}\"" }.join(',')})".toString()
            }
        }

        // Foreign Keys
        String fks = definitions.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }

        return "CREATE TABLE \"${tableName}\" (\n" + defs.join(",\n") + "\n)"
    }

    @Override
    String getTurnFK(boolean on) {
        // SQL Server disables/enables constraints per table or globally via sp_MSforeachtable
        return String.format("EXEC sp_MSforeachtable 'ALTER TABLE ? %s CONSTRAINT ALL'", on ? "CHECK" : "NOCHECK")
    }

    @Override
    String getCopyTableStructureSQL(String from, String to) {
        return "SELECT * INTO \"${to}\" FROM \"${from}\" WHERE 1=0"
    }

    @Override
    String getCopyTableDataSQL(String from, String to, TableDefinition columns) {
        return "INSERT INTO \"${to}\" SELECT * FROM \"${from}\""
    }

    @Override
    String getVersionUpdate(String table, int version) {
        // Uses SQL Server Extended Properties to store table version
        return "IF NOT EXISTS (SELECT 1 FROM fn_listextendedproperty('MS_Description', 'SCHEMA', 'dbo', 'TABLE', '${table}', NULL, NULL)) " +
        "EXEC sp_addextendedproperty 'MS_Description', 'v.${version}', 'SCHEMA', 'dbo', 'TABLE', '${table}' " +
            "ELSE EXEC sp_updateextendedproperty 'MS_Description', 'v.${version}', 'SCHEMA', 'dbo', 'TABLE', '${table}'"
    }

    @Override
    String getVersionRead(String table) {
        return "SELECT CAST(value AS VARCHAR(255)) FROM fn_listextendedproperty('MS_Description', 'SCHEMA', 'dbo', 'TABLE', '${table}', NULL, NULL)"
    }

    @Override
    String getResetAutoIncrementSQL(String tableName) {
        return "DBCC CHECKIDENT ('${tableName}', RESEED, 0)"
    }

    @Override
    String getColumnDefinition(ColumnDefinition column) {
        String type = ""
        switch (column.type) {
            case boolean:
            case Boolean:
                type = "BIT"
                break
            case char:
            case Character:
                type = "CHAR(1)"
                break
            case char[]:
                type = "CHAR(${column.length ?: 2})"
                break
            case String:
                type = column.length == UNLIMITED ? "NVARCHAR(MAX)" : "NVARCHAR(${column.length ?: 255})"
                break
            case byte:
            case Byte:
                type = "TINYINT"
                break
            case short:
            case Short:
                type = "SMALLINT"
                break
            case int:
            case Integer:
            case Model:
                type = "INT"
                break
            case BigInteger:
            case long:
            case Long:
                type = "BIGINT"
                break
            case float:
            case Float:
                type = "REAL"
                break
            case double:
            case Double:
            case BigDecimal:
                type = "FLOAT"
                break
            case LocalDate:
                type = "DATE"
                break
            case LocalDateTime:
                type = "DATETIME2"
                break
            case LocalTime:
                type = "TIME"
                break
            case Inet4Address:
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column.length ?: 45})"
                break
            case URL:
            case URI:
                type = "VARCHAR(${column.length ?: 255})"
                break
            case Collection:
            case Map:
                type = "NVARCHAR(MAX)" // SQL Server stores JSON as NVARCHAR
                break
            case Enum:
                // SQL Server doesn't have native ENUM; we use VARCHAR with CHECK constraint or just VARCHAR
                type = "VARCHAR(255)"
                break
            case byte[]:
                type = "VARBINARY(MAX)"
                break
            default:
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
                    type = "NVARCHAR(${column.length ?: 255})"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                }
        }
        return type
    }
}
