package com.intellisrc.db.jdbc

import spock.lang.Ignore

/**
 * @since 18/06/15.
 */
@Ignore //Ran manually
class MySQLTest extends JDBCTest {
    String getTableCreate(String name) {
        return """CREATE TABLE `$name` (
                `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
                `name` VARCHAR(10) NOT NULL,
                `version` FLOAT,
                `active` ENUM('N','Y'),
                `updated` DATE
        )"""
    }

    /**
     * Launch example:
     * docker run --name mysql -e MYSQL_ROOT_PASSWORD=rootpassword -e MYSQL_DATABASE=test -e MYSQL_USER=test -e MYSQL_PASSWORD=test -p 127.0.0.1:3306:3306 -d mysql
     * @return
     */
    @Override
    JDBC getDB() {
        return new MySQL(
            user    : "test",
            hostname: "localhost",
            password: "test",
            dbname  : "test"
        )
    }
}