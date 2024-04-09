package com.intellisrc.core

import com.intellisrc.core.ext.ToMap
import spock.lang.Specification

/**
 * Add toMap() and toSnakeMap() to any object
 * @since 2024/04/09.
 */
class ToMapTest extends Specification {
    static class User implements ToMap {
        String name
        int age
        double avgPct
    }
    static class TestMap implements ToMap {
        boolean ok = false
        int number = 100
        String text = "hello"
        List someList = []
    }
    def "toMap should return a Map"() {
        setup:
            User user = new User(
                name    : "Penny",
                age     : 31,
                avgPct  : 0.5d
            )
        expect:
            assert user.toMap().name == "Penny"
            assert user.toMap().age == 31
            assert user.toSnakeMap().avg_pct == 0.5d
    }
    def "toMap should return a map"() {
        setup:
        TestMap testMap = new TestMap()
        expect:
        assert testMap.toMap() == [
            ok : false,
            number : 100,
            text : "hello",
            someList : []
        ]
    }
    def "toSnakeMap should return names with snake_case"() {
        setup:
        TestMap testMap = new TestMap()
        expect:
        assert testMap.toSnakeMap() == [
            ok : false,
            number : 100,
            text : "hello",
            some_list : []
        ]
    }
}
