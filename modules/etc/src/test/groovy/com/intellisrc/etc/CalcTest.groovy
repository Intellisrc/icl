package com.intellisrc.etc

import spock.lang.Specification

/**
 * @since 2019/10/04.
 */
class CalcTest extends Specification {
    
    def "Calculate Standard Deviation"() {
        setup:
            List<Double> values = [12.5, 5.8, 78, 96, 105, 35.67]
            double result = Calc.stdDeviation(values)
            println ("STD Deviation Result: " + result)
        expect:
            assert result : "Calculation error."
    }
    
}