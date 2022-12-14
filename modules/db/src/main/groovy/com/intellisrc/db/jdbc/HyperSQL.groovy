package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * HSQLDB Database
 * @since 2022/12/5.
 *
 * Additional settings:
 * db.hypersql.*
 *
 * NOTES:
 * 1. HSQLDB auto-increment will start with 0, you will need to specify:
 *      id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY
 */
@CompileStatic
class HyperSQL extends JDBCServer {
    // Absolute path to database
    String dbname = ""
    String user = ""
    String password = ""
    String hostname = "localhost"
    int port = 0
    String packageName = "org.hsqldb.jdbc"
    String driver = "${packageName}.JDBCDriver"
    boolean supportsBoolean = true
    boolean supportsReplace = false     //TODO: it supports it, but the syntax is more complicated to implement

    @Override
    String getCatalogSearchName() {
        return null
    }

    @Override
    String getSchemaSearchName() {
        return null
    }

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }

    @Override
    String getTablesQuery() {
        return "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_type != 'VIEW' AND table_schema = 'PUBLIC' AND table_type = 'BASE TABLE'"
    }
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.hsqldb.params", [:] + params)
    }

    @Override
    String getConnectionString() {
        String conn = "hsqldb:$hostname/$port:$dbname"
        return conn + "?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
}
