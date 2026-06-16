package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 * Useful reference : https://blogs.oracle.com/sql/post/how-to-create-users-grant-them-privileges-and-remove-them-in-oracle-database
 */
class OracleSQLTest extends JDBCTest {
    /**
     * docker run -d -p 127.0.0.1:31521:1521 -e ORACLE_PASSWORD=test -e APP_USER=test -e APP_USER_PASSWORD=test -n oracle gvenzl/oracle-xe:21-slim
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     * @return
     */
    @Override
    JDBC getJdbConnector() {
        /*  Manual way:
            CREATE USER test IDENTIFIED BY test;
            GRANT ALL PRIVILEGES TO test;
         */
        return new Oracle(
            hostname: "127.0.0.1",
            port    : 31521,
            user    : "test",
            password: "test",
            dbname  : "FREEPDB1", // v23 docker
            convertToLowerCase: true
            //dbname  : "XEPDB1" // v21 PDB name (or SID)
            // For docker XE lower than 18 :
            // dbname  : "XE"
            // If you don't have `tnsnames.ora` set, you may need to specify: XEPDB1.localdomain (Specially Oracle 12)
            // dbname  : "XEPDB1.localdomain"
        )
    }
}