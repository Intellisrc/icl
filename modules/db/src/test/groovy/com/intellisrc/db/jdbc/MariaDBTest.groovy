package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class MariaDBTest extends JDBCTest {
    String getTableCreate(String name) {
        return """CREATE TABLE `$name` (
                `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
                `name` VARCHAR(10) NOT NULL,
                `version` DECIMAL(2,1),
                `active` ENUM('N','Y'),
                `updated` DATETIME
        );"""
    }

    /**
     * Launch example:
     * docker run --name mariadb -e MARIADB_ROOT_PASSWORD=rootpassword -e MARIADB_DATABASE=test -e MARIADB_USER=test -e MARIADB_PASSWORD=test -p 127.0.0.1:3306:3306 -d mariadb
     *
     * NOTE: if you are running mysql already, change the port number (e.g. 3307) or remove that container first
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new MariaDB(
            user    : "test",
            hostname: "localhost",
            password: "test",
            dbname  : "test",
            //port  : 3307
        )
    }
}