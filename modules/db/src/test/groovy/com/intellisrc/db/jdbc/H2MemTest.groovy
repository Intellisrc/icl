package com.intellisrc.db.jdbc

import org.slf4j.event.Level

/**
 * @since 18/06/15.
 */
class H2MemTest extends JDBCTest {
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
            memory: true,
        )
    }
}