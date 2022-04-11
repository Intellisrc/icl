package com.intellisrc.net

import spock.lang.Specification


/**
 * @since 17/04/19.
 */
class LocalHostTest extends Specification {
    def "Free port"() {
        setup:
            int port1 = LocalHost.getFreePort()
            int port2 = LocalHost.getFreePort()
        expect:
            assert port1 > 0 && port2 > 0
            assert port1 != port2
    }
    def "Port is available"() {
        setup:
            int port = LocalHost.getFreePort()
        expect:
            assert LocalHost.isPortAvailable(port)
    }
    def "Port not available"() {
        setup:
            int port = 1
        expect:
            assert ! LocalHost.isPortAvailable(port)
    }
}