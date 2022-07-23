package com.intellisrc.db

import com.intellisrc.db.jdbc.SQLite
import spock.lang.Specification

/**
 * @since 17/03/02.
 */
class PoolConnectorTest extends Specification {
    def "Testing return-to-pool"() {
        setup:
            Database database = new Database(new SQLite(memory: true))
        when:
            List<DB> dbArr = []
            (1..10).each {
                DB db = database.connect()
                db.openIfClosed()
                assert db: "Failed to connect to Database"
                dbArr << db
            }
        then:
            assert database.connections == 10
            assert database.pool.availableConnections.size() == 0
            assert database.pool.currentConnections.size() == 10
        when:
            (1..8).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
        then:
            assert database.connections == 2
            assert database.pool.availableConnections.size() == 8
            assert database.pool.currentConnections.size() == 2
        when:
            (1..8).each {
                DB db = database.connect()
                db.openIfClosed()
                assert db: "Failed to initialize DB"
                dbArr << db
            }
        then:
            assert database.connections == 10
            assert database.pool.availableConnections.size() == 0
            assert database.pool.currentConnections.size() == 10
        when:
            (1..10).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
        then:
            assert database.connections == 0
            assert database.pool.availableConnections.size() == 10
            assert database.pool.currentConnections.size() == 0
        cleanup:
            database.quit()
    }
    def "Testing disable Pool"() {
        setup:
            Database database = new Database(new SQLite(memory: true))
        when:
            DB db = database.connect()
            db.openIfClosed()
        then:
            assert db : "Failed to initialize DB"
            assert database.connections == 1
        when:
            DB db2 = database.connect()
            db2.openIfClosed()
        then:
            assert db2 : "Failed to initialize DB"
            assert database.connections == 2
        when:
            db.close()
            db2.close()
        then:
            assert database.connections == 0
        cleanup:
            database.quit()
    }
}