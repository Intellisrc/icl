package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class SQLServerTest extends JDBCTest {

    String getTableCreate(String name) {
        return """CREATE TABLE $name (
                id INT IDENTITY(1,1) PRIMARY KEY,
                name VARCHAR(10) NOT NULL UNIQUE,
                version FLOAT,
                active VARCHAR(5) CHECK (active IN('true','false')),
                updated DATE
        )"""
    }

    String getTableCreateMultiplePK(String name) {
        return """CREATE TABLE ${name} (
                  uid INT NOT NULL,
                  gid INT NOT NULL,
                  name VARCHAR(30) NOT NULL,
                  PRIMARY KEY (gid,uid)
        )"""
    }

    /**
     * Launch test:
     * NOTE: requires strong password
     * docker run -d --name sqlserver -e "ACCEPT_EULA=Y" -e SA_PASSWORD=o2Aksm.A23asl -p 127.0.0.1:31433:1433 mcr.microsoft.com/mssql/server:2019-latest
     * $ docker exec -it sqlserver bash
     *      cd /opt/mssql-tools/bin/
     *      ./sqlcmd -S localhost -U SA -P "o2Aksm.A23asl"
     *          1> CREATE DATABASE test
     *          2> GO
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new SQLServer(
            user    : "sa",
            hostname: "127.0.0.1",
            password: "o2Aksm.A23asl",
            dbname  : "test",
            port    : 31433
        )
    }
}