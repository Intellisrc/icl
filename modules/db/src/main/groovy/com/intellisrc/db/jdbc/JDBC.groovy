//file:noinspection GrMethodMayBeStatic
package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnInfo
import com.intellisrc.db.DB
import com.intellisrc.db.DatabaseConnectionException
import com.intellisrc.db.JDBCConnector
import com.intellisrc.db.Query
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Field
import java.sql.Connection
import java.sql.SQLNonTransientConnectionException

/**
 * Minimum JDBC information to connect to any database
 *
 * In configuration file, you can set:
 * db.type = [mysql,mariadb,postgresql,sqlite,oracle,db2,sqlserver,derby]
 * db.host = localhost
 * db.port = 1234
 * db.name = mydb
 * db.user = myuser
 * db.pass = secret
 *
 * Other databases to consider: ingres, ole db, splunk, hsql, sybase, informix.
 * @since 17/12/13.
 */
@CompileStatic
abstract class JDBC {
    interface ErrorHandler {
        void call(Throwable e)
    }
    static enum BooleanHandle {
        BOOLEAN,    // DB stores and read booleans as booleans
        NUMBER,     // DB uses 0 and 1 (no boolean alternative)
        //BIT,        // DB stores 0 and 1, but returns true / false
        CHAR,       // DB only uses string, so we store "y" or "n"
        ENUM        // DB can use ENUM("true","false")
    }
    /**
     * Override this method for custom classes
     * No need to include user/password in URL
     * as it is sent separately
     * @return
     */
    abstract String getConnectionString()

    abstract void setDbname(String name)
    abstract void setUser(String user)
    abstract void setPassword(String pass)
    abstract String getDbname()
    abstract String getUser()
    abstract String getPassword()
    /**
     * Class for driver
     * @return
     */
    abstract String getDriver()
    abstract void setDriver(String driver)

    ErrorHandler onError = {
        Throwable e ->
            Log.e("Database exception: ", e)
    } as ErrorHandler

    // Aliases
    final void setDatabase(String name) { dbname = name }
    final void setName(String database) { dbname = database }
    final void setUsername(String usrname) { user = usrname }
    final void setPass(String pwd) { password = pwd }
    final String getDatabase() { return dbname }
    final String getName() { return dbname }
    final String getUsername() { return user }
    final String getPass() { return password }

    Map params = [:] // Store params passed in constructor
    protected Map getParameters() { return params }

    // Clear the connection (used in case something is left in it that may affect reusing it later)
    void clear(Connection connection) {}

    // This will be set in case it is set directly
    protected String connectionURI = ""

    // QUERY BUILDING -------------------------------
    /**
     * Query must return (empty when not available):
     *      column, type, length, default, notnull, primary
     *
     * column, type : string (lowercase)
     * default : default value (variable)
     * primary : 0,1
     * nullable: 0,1
     * length  : numeric
     * type    : without parenthesis
     * Leave empty to use JDBC internal code
     *
     * @param table
     * @return
     */
    String getInfoQuery(String table) { "" }
    /**
     * Get last inserted ID
     * JDBC internal code will be tried first, if it fails,
     * you can provide an alternative way here
     * @param table
     * @return
     */
    String getLastIdQuery(String table, String pk) { "" }
    /**
     * Return a list of tables
     * Leave empty to use JDBC internal code
     * @return
     */
    String getTablesQuery() { "" }
    /*
     * To use in JDBC search (return null for wildcard)
     */
    String getCatalogSearchName() { return dbname }
    String getSchemaSearchName() { return "" }
    String getTableSearchName(String table) { return table }
    Set<String> filterTables(Set<String> tables) { return tables }
    String getFieldForQuery(String field) { return fieldsQuotation + (convertToLowerCase ? field.toLowerCase() : field) + fieldsQuotation }
    String getTableForQuery(String table) { return tablesQuotation + table + tablesQuotation }
    /*
     * Properties:
     * Override if its different
     */
    // Some databases require fields to be quoted, for example, MySQL uses "`"
    String getFieldsQuotation() { return "" }
    // Some databases require tables to be quoted, for example, MySQL uses "`"
    String getTablesQuotation() { return "" }
    // Some databases (like SQLite) does not support DATE type. Turn this off.
    boolean getSupportsDate() { return true }
    // Some databases (like Oracle) stores tables and fields in UpperCase, with this, all are converted into lower:
    boolean getConvertToLowerCase() { return true }
    // When false it will use LIMIT ... OFFSET
    boolean getUseFetch() { return true }
    // In cases como Oracle which MAX(column) does not include the digits, we force to check:
    boolean getCheckDecimals() { return false }
    // How do boolean will be stored in Database? (BOOLEAN == native support)
    BooleanHandle getBooleanHandle() { return BooleanHandle.BOOLEAN }
    // True char in case booleanHandle == CHAR
    char getTrueChar() { return 'y' as char }
    // False char in case booleanHandle == CHAR
    char getFalseChar() { return 'n' as char }
    // If Database supports JSON datatype
    boolean getSupportsJSON() { return false }
    // Syntax to specify column is null
    String getIsNullQuery() { return  "IS NULL" }
    // When true, it will use "replace" query, otherwise will try to update first and if it fails, will insert
    // FIXME: The best performance and thread-safe way is to implement MERGE/ON CONFLICT, however
    //        it is more complicated to implement.
    boolean getSupportsReplace() { return true }
    /*
     * DEFAULT SQL
     * Override if its different
     * In all the following methods, "table" is already quoted, if needed (added by Query)
     */
    String getCreateDatabaseQuery() {
        return "CREATE DATABASE $dbname"
    }
    String getDropDatabaseQuery() {
        return "DROP DATABASE $dbname"
    }
    String getTruncateQuery(String table) {
        return "TRUNCATE TABLE $table"
    }
    String getBeforeDropTableQuery(String table) {
        return ""
    }
    String getDropTableQuery(String table) {
        return "DROP TABLE $table"
    }
    String getAfterDropTableQuery(String table) {
        return ""
    }
    String getDropViewQuery(String view) {
        return "DROP VIEW $view"
    }
    String getInsertQuery(String table, String values) {
        return "INSERT INTO $table $values"
    }
    String getUpdateQuery(String table, String values, String where) {
        return "UPDATE $table SET $values $where"
    }
    String getDeleteQuery(String table, String where) {
        return "DELETE FROM $table $where"
    }
    String getReplaceQuery(String table, String values) {
        return "REPLACE INTO $table $values"
    }
    // hasOrder is only needed in SQL Server
    String getLimitQuery(int limit, int offset, boolean hasOrder = false) {
        return useFetch ? ((offset > 0 ? "OFFSET $offset ROWS " : "") + (limit > 0 ? "FETCH NEXT $limit ROWS ONLY" : ""))
                        : ((limit > 0 ? "LIMIT $limit " : "") + (offset > 0 ? "OFFSET $offset" : ""))
    }

    /**
     * Create the SELECT SQL Query
     * @param fields
     * @param table
     * @param where
     * @param groupBy
     * @param orderBy
     * @param offset
     * @param limit
     * @return
     */
    String getSelectQuery(String fields, String table, String where, String groupBy,  Map<String, Query.SortOrder> orderBy, int offset, int limit) {
        String query = "SELECT $fields FROM $table $where $groupBy"
        if(orderBy) {
            String orderQry = ""
            orderBy.each {
                String column, Query.SortOrder order ->
                    orderQry += (orderQry ? "," : "") + column + " " + order.toString()
            }
            query += " ORDER BY $orderQry"
        }
        if(offset || limit) {
            query += " " + getLimitQuery(limit, offset, !orderBy.keySet().empty)
        }
        return query
    }

    /**
     * In case it is needed to complete a column information
     * (may be override)
     * @param info
     * @return
     */
    ColumnInfo fillColumn(final ColumnInfo info, Map row) {
        return info
    }

    /**
     * Get database type as string based on class
     * @return
     */
    @Override
    String toString() {
        return this.class.simpleName.toLowerCase()
    }

    /**
     * Return new connection
     * @return
     */
    DB connect() throws DatabaseConnectionException {
        DB db = new DB(new JDBCConnector(this))
        db.openIfClosed()
        return db
    }

    //----------- STATIC ----------------
    /**
     * Uses a JDBCConfig object to specify connection configuration
     * @param config
     * @return
     */
    static JDBC fromSettings(JDBCConfig config) {
        return fromSettings(config.toMap())
    }
    /**
     * Will return a JDBC object from passed settings or those in config.properties
     * If settings are specified as parameter, those will be used. Otherwise will try
     * to get the values form your configuration file.
     *
     * @param settings
     * @return
     */
    static JDBC fromSettings(Map settings = [:]) {
        JDBC jdbc = null
        String cfgType = settings.type ?: Config.any.get("db.type", "dummy")
        if(settings.keySet().empty) {
            // Only set if exists:
            //noinspection GroovyMissingReturnStatement
            if (Config.any.exists("db.name")) {
                settings.dbname = Config.any.get("db.name")
            }
            if (Config.any.exists("db.host")) {
                settings.hostname = Config.any.get("db.host")
            }
            if (Config.any.exists("db.port")) {
                settings.port = Config.any.getInt("db.port")
            }
            if (Config.any.exists("db.user")) {
                settings.user = Config.any.get("db.user")
            }
            if (Config.any.exists("db.pass")) {
                settings.password = Config.any.get("db.pass")
            }
            if (Config.any.exists("db.driver")) {
                settings.driver = Config.any.get("db.driver")
            }
            if (Config.any.exists("db.params")) {
                settings.params = Config.any.getMap("db.params")
            }
        } else {
            // Allow different aliases for keys
            //noinspection GroovyMissingReturnStatement
            [
                database : "dbname",
                name     : "dbname",
                username : "user",
                pass     : "password",
                host     : "hostname"
            ].each {
                if(settings.containsKey(it.key)) {
                    settings[it.value] = settings[it.key]
                }
            }
        }
        if(cfgType) {
            jdbc = fromType(cfgType)
            if(jdbc) {
                Class cls = jdbc.class
                while (cls != Object) {
                    cls.declaredFields.findAll { !it.synthetic }.each {
                        Field field ->
                            if (settings.containsKey(field.name)) {
                                field.setAccessible(true)
                                field.set(jdbc, settings.get(field.name))
                            }
                    }
                    cls = cls.superclass
                }
            } else {
                Log.w("Unknown JDBC class for type: %s", cfgType)
            }
        } else {
            Log.e("No `type` was specified in argument or in configuration. Connection will fail.")
        }
        return jdbc
    }

    /**
     * Get an empty JDBC instance for that type
     * for example "mysql" will return an empty "MySQL" instance
     * @param type
     * @return
     */
    static JDBC fromType(String type) {
        type = switch(type) {
            case "hsqldb" -> "hypersql"
            case "firebirdsql" -> "firebird"
            default -> type.toLowerCase()
        }
        Reflections reflections = new Reflections(this.package.name)
        Set<Class<? extends JDBC>> set = reflections.getSubTypesOf(JDBC.class)
        Class<? extends JDBC> cj = set.find {
            it.simpleName.toLowerCase() == type
        }
        return cj ? cj.getConstructor().newInstance() : null
    }
    /**
     * Get a JDBC object from connection URI
     * @param uri
     * @return
     */
    static JDBC fromURI(String uri, String userName = "", char[] pwd = []) {
        String type = uri.tokenize(":").first()
        JDBC jdbc = fromType(type) ?: new JDBC() {
            String dbname = ""
            String user = ""
            String password = ""
            String driver = ""
            @Override
            String getConnectionString() {
                return uri
            }
        }
        jdbc.connectionURI = uri
        jdbc.user = userName
        jdbc.password = pwd.toString()
        return jdbc
    }
}