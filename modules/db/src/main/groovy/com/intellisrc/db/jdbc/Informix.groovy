package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * HSQLDB Database
 * @since 2022/12/5.
 *
 * Additional settings:
 * db.hypersql.
 */
@CompileStatic
class Informix extends JDBCServer {
    // Absolute path to database
    String dbname = ""
    String user = "informix"
    String password = "in4mix"
    String hostname = "localhost"
    int port = 9088
    String packageName = "com.informix.jdbc"
    String driver = "${packageName}.ifxDriver"
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
        String conn = "informix:$hostname/$port:$dbname"
        return conn + "?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
}
