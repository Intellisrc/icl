package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class MariaDBTest extends JDBCTest {

    String getTableCreate(String name) {
        //NOTE: in ENUM, 'false' is first to represent 0 ordinal (used for sorting)
        return """CREATE TABLE `$name` (
                `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
                `name` VARCHAR(10) NOT NULL,
                `version` DECIMAL(2,1),
                `active` ENUM('false','true'),
                `updated` DATETIME
        );"""
    }

    String getTableCreateMultiplePK(String name) {
        return """CREATE TABLE `${name}` (
                  `uid` SMALLINT NOT NULL,
                  `gid` SMALLINT NOT NULL,
                  `name` VARCHAR(30) NOT NULL,
                  PRIMARY KEY (`gid`,`uid`)
        )"""
    }

    /**
     * Launch example:
     * docker run --name mariadb -e MARIADB_ROOT_PASSWORD=rootpassword -e MARIADB_DATABASE=test -e MARIADB_USER=test -e MARIADB_PASSWORD=test -p 127.0.0.1:33007:3306 -d mariadb
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new MariaDB(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 33007
        )
    }
}