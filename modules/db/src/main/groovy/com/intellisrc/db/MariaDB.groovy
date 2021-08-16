package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/14.
 */
@CompileStatic
class MariaDB extends JDBC {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = DB.DBType.MARIADB.port
    @Override
    String getConnectionString() {
        return "mariadb://$user:$password@$hostname:$port/$dbname"
    }
}
