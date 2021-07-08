package com.intellisrc.etc

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import spock.lang.Specification

import java.time.LocalDate
import java.time.LocalDateTime


/**
 * @since 17/10/30.
 */
class CacheTest extends Specification {
    void setup() {
        Log.enabled = false
    }
    def "Testing cache"() {
        setup:
            def oc = CacheObj.instance
            oc.set("Hello","World")
            oc.set("Date", SysClock.now.toLocalDate())
        expect:
            assert !oc.isEmpty()
            assert oc.contains("Hello")
            assert !oc.contains("Bla")
            assert oc.get("Hello") == "World"
            assert oc.get("Date") instanceof LocalDate
        when:
            LocalDate d = oc.get("Date") as LocalDate
            println d.YMD
            oc.del("Date")
        then:
            assert !oc.contains("Date")
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
            assert oc.contains(key) : "Must exists in this point"

            sleep(2000)
            assert ! oc.contains(key) : "Must NOT exists in this point"
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
            assert oc.contains(key) : "After 2 seconds it should be there"

            sleep(2000)
            assert oc.contains(key) : "After 4 seconds it should be there as it was renewed"

            sleep(4000)
            assert ! oc.contains(key) : "After 4 seconds from the last read it should NOT be there"

        cleanup:
            oc.clear()
    }

    def "Testing Forever"() {
        setup:
            def oc = new Cache<Integer>()
            def key = "Wow"
            //   oc.set("Hello","World") <--must be marked by IDE as wrong
            oc.set(key,1) //This will be forever
        expect:
            sleep(2000)
            assert oc.contains(key) : "Must exists in this point"

            sleep(2000)
            assert oc.contains(key) : "Must exists in this point"
        cleanup:
            oc.clear()
    }

    def "Test keys"() {
        setup:
            def oc = new Cache<Integer>()
            oc.set("a",1)
            oc.set("b",2)
            oc.set("c",3)
        expect:
            assert oc.keys().size() == 3
            assert oc.keys().first() == "a"
            assert oc.keys().contains("b")
            assert oc.keys().last() == "c"
        cleanup:
            oc.clear()
    }
}