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
    boolean supportsReplace = false
    String catalogSearchName = null
    String schemaSearchName = null

    @Override
    String getTableSearchName(String table) {
        return table.toUpperCase()
    }
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.any.get("db.h2.params", [:] + params)
    }

    @Override
    String getTablesQuery() {
        return "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema = 'PUBLIC'"
    }

    @Override
    String getConnectionString() {
        return "h2:" + (hostname ? "tcp://$hostname:$port/$dbname" + "?" + parameters.toQueryString() : "h2:$dbname")
    }

    // QUERY BUILDING -------------------------
}
