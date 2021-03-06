package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.db.auto.AutoJDBC
import groovy.transform.CompileStatic

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
    boolean ssl = false
    boolean useWinLogin = false
    boolean trustCert = false
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-ver15
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.sqlserver.params", [
            encrypt : ssl,
            integratedSecurity : useWinLogin,
            loginTimeout : 15,
            trustServerCertificate : trustCert,
        ] + params)
    }

    @Override
    String getConnectionString() {
        return "sqlserver://$hostname:$port;" +
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
    String getLastIdQuery(String table) {
        return "SELECT SCOPE_IDENTITY()"
    }

    @Override
    String getLimitQuery(int limit, int offset, boolean hasOrder) {
        return (hasOrder ? "" : "ORDER BY 1 ") + "OFFSET $offset ROWS" + (limit > 0 ? " FETCH NEXT $limit ROWS ONLY" : "")
    }
}
