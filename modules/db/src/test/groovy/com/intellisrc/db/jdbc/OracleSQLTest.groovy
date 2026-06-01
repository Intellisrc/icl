package com.intellisrc.db.jdbc

import com.intellisrc.db.DB

import static com.intellisrc.db.jdbc.JDBC.BooleanHandle.NUMBER

/**
 * @since 18/06/15.
 * Useful reference : https://blogs.oracle.com/sql/post/how-to-create-users-grant-them-privileges-and-remove-them-in-oracle-database
 */
class OracleSQLTest extends JDBCTest {

    // Requires Oracle 12c+
    List<String> getTableCreateMulti(String name) {
        return [
"CREATE SEQUENCE ${name}_seq".toString(),
"""CREATE TABLE $name (
    "id" NUMBER(10,0) DEFAULT ${name}_seq.nextval PRIMARY KEY,
    "name" VARCHAR2(10) NOT NULL UNIQUE,
    "version" NUMBER(2,1), 
    "active" BOOLEAN,
    "updated" DATE
)""".toString()
        ]
        // Can also be used, but sequence table name is random:
        //return "CREATE TABLE $name (id NUMBER(10) generated as identity, name VARCHAR2(10) NOT NULL)"
    }

    String getTableCreateMultiplePK(String name) {
        // 'uid' is a reserved word in Oracle. it should be quoted
        return """CREATE TABLE ${name} (
          "uid" INT NOT NULL,
          "gid" INT NOT NULL,
          "name" VARCHAR2(30) NOT NULL,
          PRIMARY KEY ("uid","gid")
        )""".toString()
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
    JDBC getJdbConnector() {
        /*  Manual way:
            CREATE USER test IDENTIFIED BY test;
            GRANT ALL PRIVILEGES TO test;
         */
        return new Oracle(
            hostname: "127.0.0.1",
            port    : 31521,
            user    : "test",
            password: "test",
            dbname  : "FREEPDB1" // v23 docker
            //dbname  : "XEPDB1" // v21 PDB name (or SID)
            // For docker XE lower than 18 :
            // dbname  : "XE"
            // If you don't have `tnsnames.ora` set, you may need to specify: XEPDB1.localdomain (Specially Oracle 12)
            // dbname  : "XEPDB1.localdomain"
        )
    }
}