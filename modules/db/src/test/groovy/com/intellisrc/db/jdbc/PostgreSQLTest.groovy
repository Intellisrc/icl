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
                "name VARCHAR(10) NOT NULL CONSTRAINT ${name}_name_uk UNIQUE, " +
                "version FLOAT, " +
                "active BOOLEAN, " +
                "updated DATE" +
            ")"
        ]
    }

    String getTableCreateMultiplePK(String name) {
        return """CREATE TABLE ${name} (
                  uid INTEGER NOT NULL,
                  gid INTEGER NOT NULL,
                  name VARCHAR(30) NOT NULL,
                  PRIMARY KEY (gid,uid)
        )"""
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
     * docker run --name postgres -e POSTGRES_PASSWORD=randompass -POSTGRES_USER=test -POSTGRES_PASSWORD=test -p 127.0.0.1:35432:5432 -d postgres
     * Debug:
     *  $ docker exec -it postgres ash
     *      # su postgres
     *      $ psql
     *          > SELECT ...
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     * @return
     */
    @Override
    JDBC getDB() {
        return new PostgreSQL(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 35432
        )
    }
}