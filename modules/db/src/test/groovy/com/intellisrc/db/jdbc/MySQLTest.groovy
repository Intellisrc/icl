package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class MySQLTest extends JDBCTest {

    String getTableCreate(String name) {
        //NOTE: in ENUM, 'false' is first to represent 0 ordinal (used for sorting)
        return """CREATE TABLE `$name` (
                `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
                `name` VARCHAR(10) NOT NULL UNIQUE,
                `version` FLOAT,
                `active` ENUM('false','true'),
                `updated` DATE
        )"""
    }

    /**
     * Launch example:
     * docker run --name mysql -e MYSQL_ROOT_PASSWORD=rootpassword -e MYSQL_DATABASE=test -e MYSQL_USER=test -e MYSQL_PASSWORD=test -p 127.0.0.1:33006:3306 -d mysql
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     * @return
     */
    @Override
    JDBC getDB() {
        return new MySQL(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 33006
        )
    }
}