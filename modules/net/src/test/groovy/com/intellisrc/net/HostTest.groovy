package com.intellisrc.net

import com.intellisrc.core.Millis
import spock.lang.Specification

/**
 * @since 2022/04/12.
 */
class HostTest extends Specification {
    def "host is connected"() {
        expect:
            assert new Host(LocalHost.loopbackAddress).isUp()
    }

    def "ping host"() {
        setup:
            int millis = new Host(LocalHost.loopbackAddress).ping()
            long micros = new Host(LocalHost.loopbackAddress).pingMicro()
        expect:
            assert millis >= 0
            assert micros > 0
        cleanup:
            println "Ping took " + millis + " milliseconds"
            println "Ping took " + micros + " microseconds"
    }

    def "ping host fails"() {
        setup:
            // Unlikely to exists:
            int millis = new Host("10.250.254.100".toInet4Address()).ping(Millis.SECOND)
        expect:
            assert millis == -1
    }
}
