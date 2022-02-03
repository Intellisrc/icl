package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

/**
 * @since 18/06/15.
 */
class PostgreSQLTest extends JDBCTest {
    List<String> getTableCreateMulti(String name) {
        return [
            "CREATE SEQUENCE IF NOT EXISTS ${name}_seq",
            "CREATE TABLE IF NOT EXISTS $name (" +
                "id INTEGER PRIMARY KEY DEFAULT NEXTVAL ('${name}_seq'), " +
                "name VARCHAR(10) NOT NULL, " +
                "version FLOAT, " +
                "active CHAR, " + // ENUM is possible, using CREATE TYPE mybool ('N','Y'), but it will make tests more complicated
                "updated DATE" +
            ")"
        ]
    }

    @Override
    void clean(DB db, String table) {
        if(db) {
            db.setSQL("DROP SEQUENCE IF EXISTS ${table}_seq")
            (1..5).each {
                db.setSQL("DROP SEQUENCE IF EXISTS ${table}${it}_seq")
            }
        }
    }

    /**
     * Launch test:
     * docker run --name postgres -e POSTGRES_PASSWORD=randompass -POSTGRES_USER=test -POSTGRES_PASSWORD=test -p 127.0.0.1:5432:5432 -d postgres
     * Debug:
     *  $ docker exec -it postgres ash
     *      # su postgres
     *      $ psql
     *          > SELECT ...
     * @return
     */
    @Override
    JDBC getDB() {
        return new PostgreSQL(
            user    : "test",
            hostname: "localhost",
            password: "test",
            dbname  : "test"
        )
    }
}