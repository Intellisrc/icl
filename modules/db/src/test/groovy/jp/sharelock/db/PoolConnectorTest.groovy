package jp.sharelock.db

import spock.lang.Specification

/**
 * @since 17/03/02.
 */
class PoolConnectorTest extends Specification {
    final poolSize = 20
    def setup() {
    }
    def "Testing return-to-pool"() {
        setup:
            DBPool.instance.init("dummy", "dummy", poolSize)
        when:
            ArrayList<DB> dbArr = []
            (1..10).each {
                DB db = new PoolConnector().getDB()
                assert db: "Failed to initialize DB"
                dbArr << db
            }
            (1..8).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
            (1..8).each {
                DB db = new PoolConnector().getDB()
                assert db: "Failed to initialize DB"
                dbArr << db
            }
            (1..10).each {
                dbArr.first().close()
                dbArr.remove(0)
            }
        then:
            DBPool.instance.currentConnections() == 0
    }
    def "Testing disable Pool"() {
        setup:
            DBPool.instance.init("dummy","dummy",0)
        when:
            DB db = new PoolConnector().getDB()
        then:
            assert db : "Failed to initialize DB"
            assert DBPool.instance.currentConnections() == 1
        and:
            DB db2 = new PoolConnector().getDB()
        then:
            assert db2 : "Failed to initialize DB"
            assert DBPool.instance.currentConnections() == 2
        and:
            db.close()
            db2.close()
        then:
            assert DBPool.instance.currentConnections() == 0
    }
}