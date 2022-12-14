//file:noinspection GrMethodMayBeStatic
package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.db.ColumnInfo
import com.intellisrc.db.DB
import com.intellisrc.db.JDBCConnector
import com.intellisrc.db.Query
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Field
import java.sql.Connection

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
            Log.w("Database exception: %s", e.message)
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
    String getLastIdQuery(String table) { "" }
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
    List<String> filterTables(List<String> tables) { return tables }
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
    // Some databases (like Oracle) stores tables and fields in UpperCase, with this, all all converted into lower:
    boolean getConvertToLowerCase() { return true }
    // When false it will use LIMIT ... OFFSET
    boolean getUseFetch() { return true }
    // If Database supports native boolean
    boolean getSupportsBoolean() { return false }
    // Syntax to specify column is null
    String getIsNullQuery() { return  "IS NULL" }
    // When true, it will use "replace" query, otherwise will try to update first and if it fails, will insert
    // FIXME: The best performance and thread-safe way is to implement MERGE/ON CONFLICT, however
    //        it is more complicated to implement.
    boolean getSupportsReplace() { return true }
    /*
     * DEFAULT SQL
     * Override if its different
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
    String getDropTableQuery(String table) {
        return "DROP TABLE $table"
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
    DB connect() {
        return new DB(new JDBCConnector(this))
    }

    //----------- STATIC ----------------
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
        String cfgType = settings.type ?: Config.get("db.type", "dummy")
        if(settings.keySet().empty) {
            // Only set if exists:
            //noinspection GroovyMissingReturnStatement
            if (Config.exists("db.name")) {
                settings.dbname = Config.get("db.name")
            }
            if (Config.exists("db.host")) {
                settings.hostname = Config.get("db.host")
            }
            if (Config.exists("db.port")) {
                settings.port = Config.getInt("db.port")
            }
            if (Config.exists("db.user")) {
                settings.user = Config.get("db.user")
            }
            if (Config.exists("db.pass")) {
                settings.password = Config.get("db.pass")
            }
            if (Config.exists("db.driver")) {
                settings.driver = Config.get("db.driver")
            }
            if (Config.exists("db.params")) {
                settings.params = Config.getMap("db.params")
            }
        } else {
            // Allow different aliases for keys
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
            Reflections reflections = new Reflections(this.package.name)
            Set<Class<? extends JDBC>> set = reflections.getSubTypesOf(JDBC.class)
            Class<? extends JDBC> cj = set.find {
                it.simpleName.toLowerCase() == cfgType.toLowerCase()
            }
            if(cj) {
                jdbc = cj.getConstructor().newInstance()
                Class cls = cj
                while(cls != Object) {
                    cls.declaredFields.findAll { !it.synthetic }.each {
                        Field field ->
                            if(settings.containsKey(field.name)) {
                                field.setAccessible(true)
                                field.set(jdbc, settings.get(field.name))
                            }
                    }
                    cls = cls.superclass
                }
            }
        } else {
            Log.e("No `type` was specified in argument or in configuration. Connection will fail.")
        }
        return jdbc
    }
    /**
     * Get a JDBC object from connection URI
     * @param uri
     * @return
     */
    static JDBC fromURI(String uri, String userName = "", char[] pwd = []) {
        return new JDBC() {
            String dbname = ""
            String user = userName
            String password = pwd.toString()
            String driver = ""
            @Override
            String getConnectionString() {
                return uri
            }
        }
    }
}