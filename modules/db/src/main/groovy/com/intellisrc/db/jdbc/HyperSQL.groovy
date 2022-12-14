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

    @Override
    String getLastIdQuery(String table) {
        return "IDENTITY()"
    }
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.firebird.params", [
            charSet : 'utf-8'
        ] + params)
    }

    @Override
    String getConnectionString() {
        String conn = "hsqldb:$hostname/$port:$dbname"
        return conn + "?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
}
