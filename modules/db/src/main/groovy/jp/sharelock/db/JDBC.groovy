package jp.sharelock.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/13.
 */
@CompileStatic
class JDBC implements DB.Starter {
    String connectionString = ""
    @Override
    DB.Connector getNewConnection() {
        return new JDBCConnector(connectionString)
    }
}