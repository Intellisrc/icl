package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class HyperSQLTest extends JDBCTest {

    String getTableCreate(String name = "test") {
        return """CREATE TABLE $name (
                id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1, INCREMENT BY 1) PRIMARY KEY,
                name VARCHAR(10) NOT NULL UNIQUE,
                version FLOAT,
                active BOOLEAN,
                updated DATE
        );"""
    }

    String getTableCreateMultiplePK(String name) {
        return """CREATE TABLE ${name} (
                  uid INTEGER NOT NULL,
                  gid INTEGER NOT NULL,
                  name VARCHAR(30) NOT NULL,
                  PRIMARY KEY (gid,uid)
        )"""
    }

    /**
     * Launch test:
     * docker run --name=hypersql_test
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new HyperSQL(
            user    : "test",
            password: "test",
            dbname  : "test",
            port    : 39001
        )
    }
}