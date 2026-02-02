package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.db.DB
import groovy.transform.CompileStatic

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.NUMBER

/**
 * MS SQL Server
 * @since 2022/01/18.
 *
 * Additional settings:
 * db.sqlserver.params = [:]
 */
@CompileStatic
class SQLServer extends JDBCServer {
    String dbname = ""
    String user = "sa"
    String password = ""
    String hostname = "localhost"
    int port = 1433
    String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    // SQLServer specific parameters:
    // Most common:
    boolean useWinLogin = false
    boolean trustCert = false   // In production not recommended to turn 'true'
    boolean supportsJSON = true
    BooleanHandle booleanHandle = NUMBER

    //encrypt : true // for drivers 10.2 and below
    //encrypt : "strict" // For drivers 11.2 or above
    boolean secure = false
    boolean strict = false // Only if secure = true
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-ver15
    // You may add more parameters as needed (values shown below are default values)
    // Note about loginTimeout: It may drop earlier if can't reach the port so a timeout of 10, may return within 1 second
    //      in tests, it seems more like 1/5 of the time
    @Override
    Map getParameters() {
        return Config.any.get("db.sqlserver.params", [
            encrypt : secure && strict ? "strict" : secure.toString(),
            integratedSecurity : useWinLogin,
            loginTimeout : DB.connectionTimeout, //Seconds
            trustServerCertificate : trustCert,
        ] + params)
    }

    @Override
    String getConnectionString() {
        return connectionURI ?: "sqlserver://$hostname:$port;" +
            (dbname ? "database=$dbname;" : "" ) +
            parameters.collect {
                "${it.key}=${it.value}"
            }.join(";")
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String fieldsQuotation = '"'
    String schemaSearchName = "dbo"
    boolean supportsReplace = false

    @Override
    String getLastIdQuery(String table, String pk) {
        return "SELECT SCOPE_IDENTITY()"
    }

    @Override
    String getLimitQuery(int limit, int offset, boolean hasOrder) {
        return (hasOrder ? "" : "ORDER BY 1 ") + "OFFSET $offset ROWS" + (limit > 0 ? " FETCH NEXT $limit ROWS ONLY" : "")
    }
}
