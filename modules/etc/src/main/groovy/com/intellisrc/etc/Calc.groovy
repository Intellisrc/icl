package com.intellisrc.etc

import groovy.transform.CompileStatic

/**
 * @since 2019/10/03.
 */
@CompileStatic
class Calc {
    /**
     * Calculate STD deviation
     */
    static double stdDeviation(final List values, double average = 0) {
        if (!average) {
            average = (values.sum() as double) / values.size()
        }
        return Math.sqrt((((double) values.collect {
            Math.pow(((it as double) - average), 2)
        }.sum()) / (values.size() - 1d)))
    }
}
