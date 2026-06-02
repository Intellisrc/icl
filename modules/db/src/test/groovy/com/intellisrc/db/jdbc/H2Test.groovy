package com.intellisrc.db.jdbc

/**
 * @since 18/06/15.
 */
class H2Test extends JDBCTest {
    /**
     * Launch test:
     * docker run h2_test
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    @Override
    JDBC getJdbConnector() {
        return new H2(
            dbname: File.get(File.tempDir, "h2").absolutePath,
            user: "sa",
            password: ""
        )
    }
}