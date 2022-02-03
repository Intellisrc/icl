package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class DerbyTest extends JDBCTest {
    String getTableCreate(String name) {
        return """CREATE TABLE $name (
                id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                name VARCHAR(10) NOT NULL,
                version FLOAT,
                active CHAR,
                updated DATE
        )"""
    }

    /**
     * Launch test:
     * (Nothing is needed as it will run in memory).
     * To test with file, set "dbname : something"
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new Derby(
            hostname: "localhost",
            create  : true,
            memory  : true
        )
    }
}