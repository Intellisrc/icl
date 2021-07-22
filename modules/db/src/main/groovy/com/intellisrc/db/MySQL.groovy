package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/14.
 */
@CompileStatic
class MySQL extends JDBC {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = DB.DBType.MYSQL.port
    @Override
    String getConnectionString() {
        return "mysql://$user:$password@$hostname:$port/$dbname"
    }
}
