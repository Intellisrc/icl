package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * H2 Database
 * @since 2022/12/5.
 *
 * Additional settings:
 * db.h2H2.
 */
@CompileStatic
class H2 extends JDBCServer {
    // Absolute path to database
    String dbname = ""
    String user = ""
    String password = ""
    String hostname = ""
    int port = 1521
    String packageName = "org.h2"
    String driver = "${packageName}.Driver"
    boolean supportsBoolean = true

    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.firebird.params", [
            charSet : 'utf-8'
        ] + params)
    }

    @Override
    String getConnectionString() {
        String conn = "firebirdsql:$hostname/$port:$dbname"
        return conn + "?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
}
