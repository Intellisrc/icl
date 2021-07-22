package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/14.
 */
@CompileStatic
class PostgreSQL extends JDBC {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = DB.DBType.POSTGRESQL.port
    boolean ssl = false
    @Override
    String getConnectionString() {
        return "postgresql://$user:$password@$hostname:$port/$dbname" + (ssl ? "?ssl=true" : "")
    }
}
