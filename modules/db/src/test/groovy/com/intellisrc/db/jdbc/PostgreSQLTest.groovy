package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class PostgreSQLTest extends JDBCTest {
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
    JDBC getJdbConnector() {
        return new PostgreSQL(
            user    : "test",
            hostname: "127.0.0.1",
            password: "test",
            dbname  : "test",
            port    : 35432
        )
    }
}