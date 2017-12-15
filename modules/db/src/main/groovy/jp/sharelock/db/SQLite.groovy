package jp.sharelock.db

import groovy.transform.CompileStatic

/**
 * @since 17/12/14.
 */
@CompileStatic
class SQLite extends JDBC {
    String dbname = "local.db"
    @Override
    DB.Connector getNewConnection() {
        connectionString = "sqlite://$dbname"
        return new JDBCConnector(connectionString)
    }
}
