package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import groovy.transform.CompileStatic

/**
 * MariaDB Database
 * @since 17/12/14.
 *
 * Additional settings:
 * db.mariadb.params = [:]
 */
@CompileStatic
class MariaDB extends MySQL {
    String driver = "org.mariadb.jdbc.Driver"
    // MariaDB Parameters
    // https://mariadb.com/kb/en/about-mariadb-connector-j/
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.any.get("db.mariadb.params", [
            allowMultiQueries       : false,
            connectTimeout          : 0,
            socketTimeout           : 0,
            useCompression          : compression,
            useSsl                  : ssl,
            verifyServerCertificate : ! trustCert,
            autoReconnect           : true,
            //UTF-8 enable:
            useUnicode              : true,
            characterEncoding       : "UTF-8",
            characterSetResults     : "utf8",
            connectionCollation     : "utf8_general_ci",

            // These properties are not compatible with MySQL:
            //dumpQueriesOnException  : false,
            //log                     : false,
            //maxIdleTime             : 600,
            //trustServerCertificate  : false,
        ] + params)
    }
}
