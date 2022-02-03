package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * PostgreSQL Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.postgresql.params = [:]
 *
 */
@CompileStatic
class PostgreSQL extends JDBCServer {
    String dbname = ""
    String user = "postgres"
    String password = "password"
    String hostname = "localhost"
    int port = 5432
    String driver = "org.postgresql.Driver"
    // Most common:
    boolean readOnly = false
    boolean ssl = false
    // PostgreSQL specific parameters:
    // https://jdbc.postgresql.org/documentation/head/connect.html
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.postgresql.params", [
            binaryTransfer      : true,
            cleanupSavepoints   : false,
            connectTimeout      : 0,
            loginTimeout        : 0,
            readOnly            : readOnly,
            socketTimeout       : 0,
            ssl                 : ssl,
            tcpKeepAlive        : false,
        ] + params)
    }
    @Override
    String getConnectionString() {
        return "postgresql://$hostname:$port/$dbname" + (parameters.ssl ? "?ssl=true" : "")
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    boolean useFetch = false
    boolean supportsReplace = false
    /**
     * Fallback for last ID
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table) {
        return "SELECT lastval()"
    }
}
