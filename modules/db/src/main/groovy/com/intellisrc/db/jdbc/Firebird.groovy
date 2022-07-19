package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

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
    String driver = "org.firebirdsql.jdbc.FBDriver"
    boolean supportsBoolean = true

    boolean embedded = Config.get("db.firebird.embedded", false)
    boolean local = Config.get("db.firebird.local", false)
    // Firebird specific parameters:
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.firebird.params", [
            encoding : "UNICODE_FSS"
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
        return conn
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
    String getLastIdQuery(String table) {
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
}
