package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

/**
 * @since 18/06/15.
 * Useful reference : https://blogs.oracle.com/sql/post/how-to-create-users-grant-them-privileges-and-remove-them-in-oracle-database
 */
class OracleSQLTest extends JDBCTest {

    // Requires Oracle 12c+
    List<String> getTableCreateMulti(String name) {
        // Oracle doesn't support ENUM nor BOOLEAN
        return [
"CREATE SEQUENCE ${name}_seq",
"""CREATE TABLE $name (
    "id" NUMBER(10) DEFAULT ${name}_seq.nextval PRIMARY KEY,
    "name" VARCHAR2(10) NOT NULL UNIQUE,
    "version" FLOAT, 
    "active" VARCHAR(5) CHECK("active" IN('true', 'false')),
    "updated" DATE
)"""
        ]
        // Can also be used, but sequence table name is random:
        //return "CREATE TABLE $name (id NUMBER(10) generated as identity, name VARCHAR2(10) NOT NULL)"
    }

    String getTableCreateMultiplePK(String name) {
        // 'uid' is a reserved word in Oracle. it should be quoted
        return """CREATE TABLE ${name} (
          "uid" INT NOT NULL,
          "gid" INT NOT NULL,
          "name" VARCHAR(30) NOT NULL,
          PRIMARY KEY ("uid","gid")
        )"""
    }

    @Override
    void clean(DB db, String table) {
        if(db) {
            db.getSQL("SELECT LOWER(sequence_name) FROM user_sequences").toList().each {
                if(! it.toString().startsWith("iseq\$")) {
                    db.setSQL("DROP SEQUENCE ${it}")
                }
            }
        }
    }

    /**
     * docker run -d -p 127.0.0.1:31521:1521 -e ORACLE_PASSWORD=test -e APP_USER=test -e APP_USER_PASSWORD=test -n oracle gvenzl/oracle-xe:21-slim
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     * @return
     */
    @Override
    JDBC getDB() {
        /*  Manual way:
            CREATE USER test IDENTIFIED BY test;
            GRANT ALL PRIVILEGES TO test;
         */
        return new Oracle(
            hostname: "127.0.0.1",
            port    : 31521,
            user    : "test",
            password: "test",
            dbname  : "XEPDB1" // or XE (for docker XE lower than 18). It can be service name as well (SID)
        )
    }
}