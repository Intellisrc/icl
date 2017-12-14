package jp.sharelock.db

/**
 * @since 17/12/14.
 */
class MySQL extends JDBC {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = 3306
    @Override
    DB.Connector getNewConnection() {
        connectionString = "mysql://$user:$password@$hostname:$port/$dbname"
        return new JDBCConnector(connectionString)
    }
}
