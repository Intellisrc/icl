package com.intellisrc.db.jdbc

import com.intellisrc.core.Config
import com.intellisrc.core.Millis
import com.intellisrc.db.DB
import groovy.transform.CompileStatic

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.BOOLEAN

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
    // Overrides ENUM in MySQL
    BooleanHandle booleanHandle = BOOLEAN

    // MariaDB Parameters
    // https://mariadb.com/kb/en/about-mariadb-connector-j/
    // You may add more parameters as needed (values shown below are default values)
    @Override
    Map getParameters() {
        return Config.any.get("db.mariadb.params", [
            allowMultiQueries       : false,
            connectTimeout          : DB.connectionTimeout * Millis.SECOND,
            socketTimeout           : 0,
            useCompression          : compression,
            useSsl                  : ssl,
            verifyServerCertificate : ! trustCert,
            //UTF-8 enable:
            useUnicode              : true,
            characterEncoding       : "UTF-8",
            characterSetResults     : "utf8",
            ////connectionCollation     : "utf8_general_ci", <-- setting this will cause exception in more recent drivers

            //https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j
            //autoReconnect           : false, <-- Not available in 3.x+ (by design) :

            // These properties are not compatible with MySQL:
            //dumpQueriesOnException  : false,
            //maxIdleTime             : 600,
            //trustServerCertificate  : false,
        ] + params)
    }
}
