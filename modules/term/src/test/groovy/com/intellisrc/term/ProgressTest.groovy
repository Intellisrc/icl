package com.intellisrc.term

import spock.lang.Specification

/**
 * @since 19/04/15.
 */
class ProgressTest extends Specification {
    def "Test progress"() {
        when:
            int total = 500
            (0..total).each {
                Progress.summary(it, total, "So far... ")
            }
            (0..total).each {
                Progress.bar(it, total, "Done", 50)
            }
        then:
            noExceptionThrown()
    }
}