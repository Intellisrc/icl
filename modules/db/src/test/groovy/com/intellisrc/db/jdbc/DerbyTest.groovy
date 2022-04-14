package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

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

    // Issue #18
    def "Insert without ID"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            db.setSQL("""CREATE TABLE login (
                userlogin VARCHAR(10) NOT NULL,
                pass VARCHAR(64) NOT NULL
            )""")
        expect:
            assert db.table("login").insert([
                userlogin : "admin",
                pass : "somepasss"
            ])
    }

}