package jp.sharelock.db

import spock.lang.Specification


/**
 * @since 17/10/30.
 */
class CacheTest extends Specification {
    def "Testing cache"() {
        setup:
            def oc = Cache.instance
            oc.set("Hello","World")
            oc.set("Date", new Date())
        expect:
            assert ! oc.isEmpty()
            assert oc.exists("Hello")
            assert ! oc.exists("Bla")
            assert oc.get("Hello") == "World"
            assert oc.get("Date").class.isInstance(new Date())
        when:
            Date d = oc.get("Date") as Date
            println d.toStringSTD()
            oc.del("Date")
        then:
            assert !oc.exists("Date")
            assert !oc.isEmpty()
        when:
            oc.clear()
        then:
            assert oc.size() == 0
    }
}