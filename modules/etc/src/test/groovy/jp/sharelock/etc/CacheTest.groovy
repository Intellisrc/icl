package jp.sharelock.etc

import spock.lang.Specification


/**
 * @since 17/10/30.
 */
class CacheTest extends Specification {
    def "Testing cache"() {
        setup:
            def oc = CacheObj.instance
            oc.set("Hello","World")
            oc.set("Date", new Date())
        expect:
            assert !oc.isEmpty()
            assert oc.exists("Hello")
            assert !oc.exists("Bla")
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

    def "Testing cache expire"() {
        setup:
            def oc = new Cache<Integer>()
            def key = "Wow"
         //   oc.set("Hello","World") <--must be marked by IDE as wrong
            oc.set(key,1, 10)
        expect:
            //TODO: use virtual time (if possible)
            sleep(3000)
            println "Must exists in this point"
            assert oc.exists(key)
            sleep(8000)
            println "Must NOT exists in this point"
            assert ! oc.exists(key)
        cleanup:
            oc.clear()
    }
}