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
    protected final String connStr
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
        return connector.name
    }

    @Override
    DB.DBType getType() {
        return connector.type
    }

    @Override
    DB.Connector getConnector() {
        return new JDBCConnector(connectionString)
    }
}