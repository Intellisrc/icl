package jp.sharelock.db

/**
 * @since 17/12/13.
 */
class JDBC implements DB.Starter {
    String connectionString = ""
    @Override
    DB.Connector getNewConnection() {
        return new JDBCConnector(connectionString)
    }
}