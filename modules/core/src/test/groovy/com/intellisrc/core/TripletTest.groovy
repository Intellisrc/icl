package com.intellisrc.core

import spock.lang.Specification

/**
 * @since 2023/09/25.
 */
class TripletTest extends Specification {
    def "should hold 3 different kind of data"() {
        setup:
            Triplet<Double, Integer, String> triplet = new Triplet<>(2.0d, 3, "Hello")
        expect:
            assert triplet.first == 2.0d
            assert triplet.last == "Hello"
            assert triplet.middle == 3
            println triplet.toString()
    }
    def "Two triplets should be comparable"() {
        setup:
            Triplet<Float, Float, Float> triplet1 = new Triplet<>(1.5f, 1.2f, 1.0f)
            Triplet<Float, Float, Float> triplet2 = new Triplet<>(1.5f, 1.2f, 1.0f)
        expect:
            assert triplet1 == triplet2
    }
}
