package com.intellisrc.core

import spock.lang.Specification

/**
 * @since 2023/09/25.
 */
class QuadrupletTest extends Specification {
    def "should hold 4 different kind of data"() {
        setup:
            Quadruplet<Double, Integer, String, String> quadruplet = new Quadruplet<>(2.0d, 3, "Hello", "World")
        expect:
            assert quadruplet.first == 2.0d
            assert quadruplet.middleFirst == 3
            assert quadruplet.middleLast == "Hello"
            assert quadruplet.last == "World"
            println quadruplet.toString()
    }
    def "Two quadruplets should be comparable"() {
        setup:
            Quadruplet<Float, Float, Float, Float> quadruplet1 = new Quadruplet<>(1.5f, 1.2f, 1.0f, 5.0f)
            Quadruplet<Float, Float, Float, Float> quadruplet2 = new Quadruplet<>(1.5f, 1.2f, 1.0f, 5.0f)
        expect:
            assert quadruplet1 == quadruplet2
    }
}
