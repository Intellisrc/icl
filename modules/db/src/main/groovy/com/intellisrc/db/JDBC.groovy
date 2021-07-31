package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * Class used to connect to other JDBC databases
 * not included in: MySQL, PostgreSQL, SQLite
 *
 * @since 17/12/13.
 */
@CompileStatic
class JDBC implements DB.Starter {
    protected String connStr
    JDBC(String connectionString = "") {
        connStr = connectionString
    }
    /**
     * Override this method for custom classes
     * @return
     */
    @Override
    String getConnectionString() {
        return connStr
    }
    @Override
    String getName() {
        return new JDBCConnector(connectionString).name
    }
    @Override
    DB.Connector getNewConnection() {
        return new JDBCConnector(connectionString)
    }
}