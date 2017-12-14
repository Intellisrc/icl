package jp.sharelock.db

/**
 * @since 17/12/14.
 */
class SQLite extends JDBC {
    String dbname = "local.db"
    @Override
    DB.Connector getNewConnection() {
        connectionString = "sqlite://$dbname"
        return new JDBCConnector(connectionString)
    }
}
