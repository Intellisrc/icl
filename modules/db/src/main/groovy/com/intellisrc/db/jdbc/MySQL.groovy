package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * MySQL Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.mysql.params = [:]
 */
@CompileStatic
class MySQL extends JDBCServer {
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = 3306
    String driver = "com.mysql.cj.jdbc.Driver"
    // Most common parameters:
    boolean compression = false
    boolean ssl = false
    boolean trustCert = true

    // MySQL Parameters
    // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-configuration-properties.html
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.get("db.mysql.params", [
            allowMultiQueries       : false,
            connectTimeout          : 0,
            socketTimeout           : 0,
            useCompression          : compression,
            useSSL                  : ssl,
            verifyServerCertificate : ! trustCert,
            autoReconnect           : true,
            //UTF-8 enable:
            useUnicode              : true,
            characterEncoding       : "UTF-8",
            characterSetResults     : "utf8",
            connectionCollation     : "utf8_general_ci",

            // These properties are not compatible with MariaDB:
            //emptyStringsConvertToZero : true,
            //paranoid                : false,
            //requireSSL              : false,
            //useTimezone             : false,
            //useUnicode              : true,
        ] + params)
    }

    @Override
    String getConnectionString() {
        String proto = this.toString() // mysql or mariadb
        return "$proto://$hostname:$port/$dbname?" + parameters.toQueryString()
    }

    // QUERY BUILDING -------------------------
    // Query parameters
    String fieldsQuotation = '`'
    boolean useFetch = false

    /**
     * Fallback Query to get last ID
     * @param table
     * @return
     */
    @Override
    String getLastIdQuery(String table) {
        return "SELECT LAST_INSERT_ID() as lastid"
    }
}
