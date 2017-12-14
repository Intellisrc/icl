package jp.sharelock.db

import spock.lang.Specification

/**
 * @since 17/03/02.
 */
class PoolConnectorTest extends Specification {
    def setup() {
    }
    def "Testing return-to-pool"() {
        setup:
            Database.init(new Dummy())
        when:
            ArrayList<DB> dbArr = []
            (1..10).each {
                DB db = Database.connect()
                assert db: "Failed to connect to Database"
                dbArr << db
            }
            (1..8).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
            (1..8).each {
                DB db = Database.connect()
                assert db: "Failed to initialize DB"
                dbArr << db
            }
            (1..10).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
        then:
            Database.connections == 0
        cleanup:
            Database.quit()
    }
    def "Testing disable Pool"() {
        setup:
            Database.init(new Dummy())
        when:
            DB db = Database.connect()
        then:
            assert db : "Failed to initialize DB"
            assert Database.connections == 1
        and:
            DB db2 = Database.connect()
        then:
            assert db2 : "Failed to initialize DB"
            assert Database.connections == 2
        and:
            db.close()
            db2.close()
        then:
            assert Database.connections == 0
        cleanup:
            Database.quit()
    }
}