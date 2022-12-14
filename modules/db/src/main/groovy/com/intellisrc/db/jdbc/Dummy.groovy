package com.intellisrc.db.jdbc

import groovy.transform.CompileStatic
/**
 * Dummy JDBC connector
 * @since 17/12/13.
 */
@CompileStatic
class Dummy extends JDBC {
    String dbname = ""
    String user = ""
    String password = ""
    String driver = ""
    @Override
    String getConnectionString() {
        return "dummy://dummy"
    }

    @Override
    String getInfoQuery(String table) {
        return ""
    }

    @Override
    String getLastIdQuery(String table, String pk) {
        return ""
    }

    @Override
    String getTablesQuery() {
        return ""
    }
}
