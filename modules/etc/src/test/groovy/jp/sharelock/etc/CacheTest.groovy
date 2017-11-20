package jp.sharelock.etc

import spock.lang.Specification


/**
 * @since 17/10/30.
 //TODO: use fake clock (if possible)
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
            oc.set(key,1, 3)
        expect:
            sleep(2000)
            assert oc.exists(key) : "Must exists in this point"

            sleep(2000)
            assert ! oc.exists(key) : "Must NOT exists in this point"
        cleanup:
            oc.clear()
    }

    def "Testing Time extension"() {
        setup:
            def oc = new Cache<Integer>(extend: true)
            def key = "Wow"
            oc.set(key,1, 3)
        expect:
            sleep(2000)
            assert oc.exists(key) : "After 2 seconds it should be there"

            sleep(2000)
            assert oc.exists(key) : "After 4 seconds it should be there as it was renewed"

            sleep(4000)
            assert ! oc.exists(key) : "After 4 seconds from the last read it should NOT be there"

        cleanup:
            oc.clear()
    }
}