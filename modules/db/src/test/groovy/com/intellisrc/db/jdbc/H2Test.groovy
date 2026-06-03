package com.intellisrc.db.jdbc

import org.slf4j.event.Level

/**
 * @since 18/06/15.
 */
class H2Test extends JDBCTest {
    static File h2Tmp = File.get(File.tempDir, "h2.db")
    /**
     * Launch test:
     * docker run h2_test
     *
     * You can use the `launch_dbs_for_testing.sh` script located in /modules/db/ to launch it.
     *
     * @return
     */
    Level logLevel = Level.TRACE

    @Override
    JDBC getJdbConnector() {
        return new H2(
            dbname: h2Tmp.absolutePath,
            user: "sa",
            password: ""
        )
    }
}