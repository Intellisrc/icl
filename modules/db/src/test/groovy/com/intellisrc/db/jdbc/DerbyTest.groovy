package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

import java.sql.DriverManager
import java.sql.SQLException

/**
 * @since 18/06/15.
 */
class DerbyTest extends JDBCTest {
    final static File derbyLog = File.get("derby.log")
    // We don't use create directory here or Derby will fail the first test:
    final static File dbDir = File.get(File.tempDir, "derby-" + System.currentTimeMillis())
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
            create  : true,
            dbname  : dbDir.absolutePath,
        )
    }

    def cleanup() {
        // Shut down the specific database to release file locks
        try {
            if(dbDir.exists()) {
                DriverManager.getConnection("jdbc:derby:${dbDir.absolutePath};shutdown=true")
            }
        } catch (SQLException ignored) {
            // Derby always throws an SQL State 08006 or XJ015 on successful shutdown
        }
        if(derbyLog.exists()) {
            derbyLog.delete()
        }
        if(dbDir.exists()) {
            dbDir.deleteDir()
        }
    }

    // Issue #18
    def "Insert without ID"() {
        setup:
            JDBC jdbc = getDB()
            DB db = jdbc.connect()
            println "Creating table 'login' ..."
            db.setSQL("""CREATE TABLE login (
                "userlogin" VARCHAR(10) NOT NULL,
                "pass" VARCHAR(64) NOT NULL
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
            println "Creating table 'front' ..."
            db.setSQL("""
            CREATE TABLE front (
                "id" INTEGER GENERATED ALWAYS AS IDENTITY CONSTRAINT front_pk PRIMARY KEY,
                "name" VARCHAR(10),
                "port" INT DEFAULT 0,
                "mode" VARCHAR(255),
                "extra" CLOB DEFAULT '[]'
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