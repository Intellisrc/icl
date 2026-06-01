package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.DB
import com.intellisrc.db.Query
import com.intellisrc.db.ColumnDefinition
import com.intellisrc.db.TableDefinition
import com.intellisrc.db.Volatile
import com.intellisrc.db.auto.AutoJDBC
import com.intellisrc.db.auto.Model
import groovy.transform.CompileStatic
import javassist.Modifier

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static com.intellisrc.db.auto.Relational.getColumnName
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
class Derby extends JDBCServer implements AutoJDBC, Volatile {
    static enum SubProtocol {
        SERVER, DIRECTORY, MEMORY, CLASSPATH, JAR
    }
    String dbname = ""
    String user = ""
    String password = ""
    String hostname = ""
    int port = 1527
    String driver = "org.apache.derby.iapi.jdbc.AutoloadedDriver"
    boolean encrypt = Config.any.get("db.derby.encrypt", false)
    boolean memory = Config.any.get("db.derby.memory", false)
    boolean embedded = Config.any.get("db.derby.embedded", false)
    boolean create = memory ?: Config.any.get("db.derby.create", false) //If in memory it will create automatically
    boolean useFK = Config.any.get("db.derby.fk", false)
    boolean useVersion = false
    String tableMeta = Config.any.get("db.derby.meta", "sys_meta")
    SubProtocol subProtocol = DIRECTORY

    // QUERY BUILDING -------------------------
    // Query parameters
    String catalogSearchName = "%"
    String schemaSearchName = "%"
    String fieldsQuotation = '"'
    boolean supportsReplace = false

    protected boolean notifiedFkWarning = false

    // Derby specific parameters:
    // https://db.apache.org/derby/docs/10.0/manuals/reference/sqlj238.html#HDRSII-ATTRIB-24612
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        if(create) {
            params.create = create
        }
        return Config.any.get("db.derby.params", [ :
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
        if(connectionURI) {
            return connectionURI
        }
        // Do not set unless is enabled:
        if(encrypt) {
            parameters.dataEncryption = true
        }
        if(memory) {
            subProtocol = MEMORY
        } else if(embedded) {
            subProtocol = JAR
        } else if(!hostname) {
            subProtocol = DIRECTORY
        } else {
            subProtocol = SERVER
        }
        //noinspection GroovyFallthrough
        switch (subProtocol) {
            case SERVER:
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
    boolean exists(String tableName) {
        return get("SELECT TRUE FROM SYS.SYSTABLES WHERE TABLENAME = '${getTableSearchName(tableName)}' AND TABLETYPE = 'T'").toBool()
    }
    @Override
    void autoInit() {
        if(useVersion &&! exists(tableMeta)) {
            createTable(tableMeta, [
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
    boolean createTable(String tableName, TableDefinition definitions = [] as TableDefinition, String charset = "", String engine = "", int version = 1) {
        List<String> defs = []
        List<ColumnDefinition> pks = definitions.pks
        boolean isMultiplePks = definitions.hasMultiplePk()

        // 1. Process Columns
        definitions.each {
            ColumnDefinition col ->
                List<String> parts = ["\"${col.name}\"".toString(), getColumnDefinitionCustom(col)]

                // Derby Identity (Auto-Increment) logic
                if (col.autoIncrement) {
                    parts << "GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)"
                }

                // Inline Primary Key for a single PK column
                if (col.primaryKey && !isMultiplePks) {
                    parts << "PRIMARY KEY"
                }

                // Not null
                if (col.primaryKey || col.unique || (col.uniqueGroup && !col.uniqueGroup.trim().empty)) {
                    parts << "NOT NULL"
                } else if (!col.nullable) {
                    parts << "NOT NULL"
                }

                if (col.defaultValue) {
                    // Derby prefers default constraints enclosed in parenthesis for expressions, false for literals
                    parts << getDefaultQuery(col, false)
                }

                // Derby requires NOT NULL for ANY unique column
                boolean isPartofUniqueGroup = (col.uniqueGroup != null && !col.uniqueGroup.trim().empty)
                if (col.unique && !isPartofUniqueGroup) {
                    parts << "UNIQUE"
                }

                defs << parts.join(' ')
        }

        // 2. Append Composite Primary Key Constraints (if any)
        if (isMultiplePks) {
            defs << "PRIMARY KEY (" + pks.collect { "\"${it.name}\"" }.join(",") + ")"
        }

        // 3. Append Composite Unique Group Constraints
        Map<String, List<String>> uniqueGroups = definitions.uniqueGroups
        if (!uniqueGroups.empty) {
            uniqueGroups.each { String groupName, List<String> columns ->
                // In Derby, you name table constraints using the CONSTRAINT keyword
                defs << "CONSTRAINT \"${groupName}\" UNIQUE (\"${columns.join('\", \"')}\")".toString()
            }
        }

        // 4. Append Foreign Keys
        String fks = definitions.collect { getForeignKey(tableName, it) }.findAll { it }.join(",\n")
        if (fks) {
            defs << fks
        }

        // 5. Assemble Final Statement (No Engine, Charset, or Comment suffixes)
        String createSQL = "CREATE TABLE \"${getTableSearchName(tableName)}\" (\n" + defs.join(",\n") + "\n)"

        DB db = connect()
        boolean ok = db.set(new Query(createSQL))
        if (!ok) {
            Log.v(createSQL)
            Log.e("Unable to create table.")
        }
        db.close()
        return ok
    }

    /**
     * Derby uses double-quotes `"` for escaping instead of backticks `` ` ``
     */

    @Override
    boolean turnFK(boolean on) {
        return true //Not supported: https://www.mail-archive.com/derby-user@db.apache.org/msg05345.html
    }

    @Override
    boolean copyTableStructure(String from, String to) {
        return false // set(db, "CREATE TABLE ${to} AS SELECT * FROM ${from} WITH NO DATA")
    }
    @Override
    boolean copyTableData(String from, String to, TableDefinition columns) {
        boolean ok = set("INSERT INTO ${to} SELECT * FROM ${from}")
        ColumnDefinition ai = columns.find { it.autoIncrement }
        if(ai) {
            int max = get("SELECT (MAX(${ai.name}) + 1) AS m FROM ${from}").toInt()
            set("ALTER TABLE ${to} ALTER COLUMN ${ai.name} RESTART WITH ${max ?: 1}")
        }
        return ok
    }
    @Override
    boolean setVersion(String dbname, String table, int version) {
        useVersion = true
        DB db = connect()
        boolean ok = db.table(tableMeta).replace([
            table_name : table,
            version : version
        ])
        db.close()
        return ok
    }
    @Override
    int getVersion(String dbname, String table) {
        int ver = 1
        if(useVersion) {
            DB db = connect()
            ver = db.table(tableMeta).field("version").get(table_name: table).toInt()
            db.close()
        }
        return ver
    }


    @Override
    String getColumnDefinition(final ColumnDefinition column) {
        String type = ""
        //noinspection GroovyFallthrough
        switch (column.type) {
            case boolean:
            case Boolean:
                type = "BOOLEAN"
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
                type = column.length > 32672 ? "CLOB" : "VARCHAR(${column.length ?: 255})"
                break
                // All numeric values share unsigned/autoincrement and primary instructions:
            case byte:
            case Byte:
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
            case Inet4Address:
                type = "VARCHAR(${column.length ?: 15})"
                break
            case Inet6Address:
            case InetAddress:
                type = "VARCHAR(${column.length ?: 45})"
                break
            case URL:
            case URI:
            case Collection:
            case Map:
                type = column.index || column.unique || (column.length ?: 256) <= 255 ? "VARCHAR(${column.length ?: 255})" : "CLOB"
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
                    int len = column.length ?: 256
                    type = len < 256 ? "VARCHAR($len)" : "CLOB"
                } else {
                    Log.w("Unknown field type: %s", column.type.simpleName)
                    Log.v("If you want to able to use '%s' type in the database, either set fromString " +
                        "as static method or set a constructor which accepts String", column.type.simpleName)
                }
        }
        return type
    }

    @Override
    boolean copyAutoIncrement(String tableFrom, String tableTo, String columnName) {
        DB db = connect()
        boolean ok = false
        String uTableFrom = getTableSearchName(tableFrom)
        String uTableTo = getTableSearchName(tableTo)
        String uColumn = columnName.toUpperCase()

        // 1. Drill down into Derby's catalogs to look up the exact tracking pointer value
        String sql = "SELECT s.CURRENTVALUE FROM SYS.SYSSEQUENCES s " +
            "JOIN SYS.SYSCOLUMNS c ON c. Royal_Sequence_ID_Property_Matches = s.SEQUENCEID " + // Abstracted conceptual join
            "JOIN SYS.SYSTABLES t ON t.TABLEID = c.REFERENCEID " +
            "WHERE t.TABLENAME = '${uTableFrom}' AND c.COLUMNNAME = '${uColumn}'"

        // Derby fallback fallback strategy: directly query the source sequence properties
        String derbySql = "SELECT CURRENTVALUE FROM SYS.SYSSEQUENCES WHERE SEQUENCENAME = " +
            "(SELECT SEQUENCENAME FROM SYS.SYSCOLUMNS c JOIN SYS.SYSTABLES t ON c.REFERENCEID = t.TABLEID " +
            "WHERE t.TABLENAME = '${uTableFrom}' AND c.COLUMNNAME = '${uColumn}')"

        long valObj = db.get(new Query(derbySql)).toLong()
        if (valObj) {
            long nextValue = (valObj as Long) + 1
            // 2. Use RESTART WITH to realign the target table sequence mapping
            ok = db.set(new Query("ALTER TABLE \"${uTableTo}\" ALTER COLUMN \"${uColumn}\" RESTART WITH ${nextValue}"))
        }
        db.close()
        return ok
    }

    @Override
    String getForeignKey(String tableName, ColumnDefinition column) {
        String indices = ""
        if(useFK) {
            if(!notifiedFkWarning) {
                Log.w("Warning: Derby won't update correctly when using foreign keys (because they can not be turned off).")
                notifiedFkWarning = true
            }
            switch (column.type) {
                case Model:
                    Constructor<?> ctor = column.type.class.getConstructor()
                    Model refType = (ctor.newInstance() as Model)
                    String joinTable = refType.tableName
                    String action = column.ondelete
                    indices = "FOREIGN KEY (${column.name}) " +
                        "REFERENCES ${joinTable}(${getColumnName(refType.primaryKey)}) ON DELETE ${action}"
                    break
            }
        } else {
            if(! notifiedFkWarning) {
                Log.w("Foreign keys are OFF. This makes automatic updates possible, but you will need to remove references manually.")
                notifiedFkWarning = true
            }
        }
        return indices
    }
    @Override
    boolean renameTable(String from, String to) {
        return set("RENAME TABLE ${getTableSearchName(from)} TO ${getTableSearchName(to)}")
    }

    @Override
    String getTruncateQuery(String table) {
        return super.getDeleteQuery(getTableSearchName(table), "")
    }
}