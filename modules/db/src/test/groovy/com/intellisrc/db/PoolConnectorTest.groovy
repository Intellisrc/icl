package com.intellisrc.db

import spock.lang.Specification

/**
 * @since 17/03/02.
 */
class PoolConnectorTest extends Specification {
    def "Testing return-to-pool"() {
        setup:
            Database database = new Database(new Dummy())
        when:
            List<DB> dbArr = []
            (1..10).each {
                DB db = database.connect()
                assert db: "Failed to connect to Database"
                dbArr << db
            }
            (1..8).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
            (1..8).each {
                DB db = database.connect()
                assert db: "Failed to initialize DB"
                dbArr << db
            }
            (1..10).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
        then:
            database.connections == 0
        cleanup:
            database.quit()
    }
    def "Testing disable Pool"() {
        setup:
            Database database = new Database(new Dummy())
        when:
            DB db = database.connect()
        then:
            assert db : "Failed to initialize DB"
            assert database.connections == 1
        and:
            DB db2 = database.connect()
        then:
            assert db2 : "Failed to initialize DB"
            assert database.connections == 2
        and:
            db.close()
            db2.close()
        then:
            assert database.connections == 0
        cleanup:
            database.quit()
    }
}