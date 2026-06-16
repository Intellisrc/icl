package com.intellisrc.db.jdbc

import org.slf4j.event.Level

/**
 * @since 18/06/15.
 */
class MySQLTest extends JDBCTest {
    /**
     * Launch example:
     * docker run --name mysql -e MYSQL_ROOT_PASSWORD=rootpassword -e MYSQL_DATABASE=test -e MYSQL_USER=test -e MYSQL_PASSWORD=test -p 127.0.0.1:33006:3306 -d mysql
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     * @return
     */
    Level logLevel = Level.DEBUG

    @Override
    JDBC getJdbConnector() {
        return new MySQL(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 33006
        )
    }
}