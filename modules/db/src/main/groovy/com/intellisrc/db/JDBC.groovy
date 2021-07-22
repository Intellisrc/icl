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
    String connectionString
    JDBC(String connectionString = "") {
        this.connectionString = connectionString
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