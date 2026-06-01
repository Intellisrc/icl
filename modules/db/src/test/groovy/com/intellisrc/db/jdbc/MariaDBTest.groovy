package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class MariaDBTest extends JDBCTest {
    /**
     * Launch example:
     * docker run --name mariadb -e MARIADB_ROOT_PASSWORD=rootpassword -e MARIADB_DATABASE=test -e MARIADB_USER=test -e MARIADB_PASSWORD=test -p 127.0.0.1:33007:3306 -d mariadb
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    @Override
    JDBC getJdbConnector() {
        return new MariaDB(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 33007
        )
    }
}