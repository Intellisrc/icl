package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

/**
 * @since 18/06/15.
 */
class DerbyTest extends JDBCTest {
    File derbyLog = File.get("derby.log")

    String getTableCreate(String name) {
        return """CREATE TABLE $name (
                id INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                name VARCHAR(10) NOT NULL UNIQUE,
                version FLOAT,
                active BOOLEAN,
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
            memory  : true,
            port    : 0 //Do not skip test
        )
    }

    def cleanup() {
        if(derbyLog.exists()) {
            derbyLog.delete()
        }
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

    def "Create table"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            db.setSQL("""
            CREATE TABLE front (
                id INTEGER GENERATED ALWAYS AS IDENTITY CONSTRAINT front_pk PRIMARY KEY,
                name VARCHAR(10),
                port INT DEFAULT 0,
                mode VARCHAR(255),
                extra CLOB DEFAULT '[]'
            )""")
        expect:
            assert db.table("front").exists()
            assert db.table("front").insert([
                name : "Name",
                port : 100,
                mode : "http",
                extra : "----------"
            ])
            assert db.table("front").field("port").get(1).toInt() == 100

    }

}