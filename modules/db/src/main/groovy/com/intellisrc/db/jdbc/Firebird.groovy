package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import java.lang.reflect.Method
import java.sql.Connection

/**
 * Firebird SQL Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.firebird.params = [:]
 */
@CompileStatic
class Firebird extends JDBCServer {
    // Absolute path to database
    String dbname = ""
    String user = "sysdba"
    String password = "masterkey"
    String hostname = "localhost"
    int port = 3050
    String packageName = "org.firebirdsql.jdbc"
    String driver = "${packageName}.FBDriver"
    String connectionClass = "${packageName}.FBConnection"
    boolean supportsBoolean = true

    boolean embedded = Config.get("db.firebird.embedded", false)
    boolean local = Config.get("db.firebird.local", false)
    // Firebird specific parameters:
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.firebird.params", [
            charSet : 'utf-8'
        ] + params)
    }

    @Override
    String getConnectionString() {
        String conn
        switch (true) {
            // (To connect to local database)
            case local:
                conn = "firebirdsql:local:$dbname"
                break
            //(embedded server) NOTE: not thread safe
            case embedded:
                conn = "firebirdsql:embedded:$dbname"
                break
            default:
                conn = "firebirdsql:$hostname/$port:$dbname"
        }
        return conn + "?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    @Override
    String getCatalogSearchName() {
        return null
    }

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }
    /**
     * However, it is not certain that the value you'd get is the one you used as other users might have inserted new rows.
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table, String pk) {
        return "SELECT gen_id(GENERATOR_NAME, 0) FROM rdb\$database"
    }

    @Override
    String getReplaceQuery(String table, String values) {
        return "UPDATE OR INSERT INTO ${table} ${values}"
    }

    @Override
    String getTruncateQuery(String table) {
        return "DELETE FROM ${table}"
    }

    /**
     * After executing queries, Firebird leaves the statements (dirty connection)
     * Here we call `freeStatements` to be sure they are cleared.
     * @param connection
     */
    @Override
    void clear(Connection connection) {
        try {
            Class<?> clazz = Class.forName(connectionClass)
            Method free = clazz.getDeclaredMethod("freeStatements")
            free.setAccessible(true)
            free.invoke(connection)
        } catch(Exception e) {
            Log.w("Unable to free statements: %s", e.message)
        }
    }
}
