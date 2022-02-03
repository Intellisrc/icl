package com.intellisrc.db.jdbc

import spock.lang.Ignore

/**
 * @since 18/06/15.
 */
@Ignore //Ran manually
class SQLServerTest extends JDBCTest {
    String getTableCreate(String name) {
        return """CREATE TABLE $name (
                id INT IDENTITY(1,1) PRIMARY KEY,
                name VARCHAR(10) NOT NULL,
                version FLOAT,
                active CHAR,
                updated DATE
        )"""
    }

    /**
     * Launch test:
     * NOTE: requires strong password
     * docker run -d --name sqlserver -e "ACCEPT_EULA=Y" -e SA_PASSWORD=o2Aksm.A23asl -e -p 127.0.0.1:1433:1433 mcr.microsoft.com/mssql/server:2019-latest
     * $ docker exec -it sqlserver bash
     *      cd /opt/mssql-tools/bin/
     *      ./sqlcmd -S localhost -U SA -P "o2Aksm.A23asl"
     *          1> CREATE DATABASE test
     *          2> GO
     *
     * @return
     */
    @Override
    JDBC getDB() {
        return new SQLServer(
            user    : "sa",
            hostname: "localhost",
            password: "o2Aksm.A23asl",
            dbname  : "test"
        )
    }
}