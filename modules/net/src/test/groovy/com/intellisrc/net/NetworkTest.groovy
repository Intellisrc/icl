package com.intellisrc.net

import spock.lang.Specification

/**
 * @since 2022/11/09.
 */
class NetworkTest extends Specification {

    def "Check if IP is from a Local Network"() {
        expect:
            assert Network.isIpLocal("172.20.0.1".toInet4Address())
            assert Network.isIpLocal("10.0.0.1".toInet4Address())
            assert Network.isIpLocal("192.168.0.1".toInet4Address())
            assert ! Network.isIpLocal("122.0.0.1".toInet4Address())
            assert ! Network.isIpLocal("172.0.0.1".toInet4Address())
    }

    def "Get a random IP4 address"() {
        expect:
            Inet4Address ip = Network.anyRandomIP4()
            println ip.hostAddress
            assert ip
    }

    def "Get IPs from range"() {
        expect:
            List list = Network.getIpsFromRange("10.0.0.1-5").collect { it.hostAddress }
            assert list.size() == 5
            assert list.contains("10.0.0.3")
    }

    def "Network 24"() {
        expect:
            assert net.contains("192.168.1.2".toInet4Address())
            assert net.local
            assert ! net.loopBack
            assert net.address.hostAddress == "192.168.1.10"
            assert net.prevAddress.hostAddress == "192.168.1.9"
            assert net.nextAddress.hostAddress == "192.168.1.11"
            assert net.network.hostAddress == "192.168.1.0"
            assert net.firstInNetwork.hostAddress == "192.168.1.1"
            assert net.lastInNetwork.hostAddress == "192.168.1.254"
            assert net.broadcast.hostAddress == "192.168.1.255"
            assert net.netmask == "255.255.255.0"
            assert net.netmask4.hostAddress == "255.255.255.0"
            assert net.allInNetwork.size() == net.size
            assert net.contains(net.randomIP4())
            assert net.cidr == "192.168.1.10/24" && net.cidr == net.toString()
            assert net.reset().cidr == "192.168.1.0/24"
        where:
            net                                                 | unused
            new Network("192.168.1.10".toInet4Address(), 24)    | false
            new Network("192.168.1.10", 24)                     | false
            new Network("192.168.1.10/24")                      | false
            Network.fromString("192.168.1.10/24")               | false
    }

    def "Network 16"() {
        expect:
            assert net.contains("192.168.0.20".toInet4Address())
            assert net.local
            assert ! net.loopBack
            assert net.address.hostAddress == "192.168.1.100"
            assert net.prevAddress.hostAddress == "192.168.1.99"
            assert net.nextAddress.hostAddress == "192.168.1.101"
            assert net.network.hostAddress == "192.168.0.0"
            assert net.firstInNetwork.hostAddress == "192.168.0.1"
            assert net.lastInNetwork.hostAddress == "192.168.255.254"
            assert net.broadcast.hostAddress == "192.168.255.255"
            assert net.netmask == "255.255.0.0"
            assert net.netmask4.hostAddress == "255.255.0.0"
            assert net.allInNetwork.size() == net.size && net.size == 65534
            assert net.contains(net.randomIP4())
            assert net.cidr == "192.168.1.100/16" && net.cidr == net.toString()
            assert net.reset().cidr == "192.168.0.0/16"
        where:
            net                                                 | unused
            new Network("192.168.1.100".toInet4Address(), 16)   | false
            new Network("192.168.1.100", 16)                    | false
            new Network("192.168.1.100/16")                     | false
            Network.fromString("192.168.1.100/16")              | false
    }

    def "Network 32"() {
        expect:
            assert ! net.contains("192.168.1.15".toInet4Address())
            assert net.local
            assert ! net.loopBack
            assert net.address.hostAddress == "192.168.1.10"
            assert net.prevAddress.hostAddress == "192.168.1.9"
            assert net.nextAddress.hostAddress == "192.168.1.11"
            assert net.network.hostAddress == "192.168.1.10"
            assert net.firstInNetwork.hostAddress == "192.168.1.10"
            assert net.lastInNetwork.hostAddress == "192.168.1.10"
            assert net.broadcast.hostAddress == "192.168.1.10"
            assert net.netmask == "255.255.255.255"
            assert net.netmask4.hostAddress == "255.255.255.255"
            assert net.allInNetwork.size() == net.size && net.size == 1
            assert net.contains(net.randomIP4())
            assert net.cidr == "192.168.1.10/32" && net.cidr == net.toString()
        where:
            net                                                 | unused
            new Network("192.168.1.10".toInet4Address())        | false
            new Network("192.168.1.10")                         | false
            new Network("192.168.1.10/32")                      | false
            Network.fromString("192.168.1.10/32")               | false
    }

    def "Reset Network"() {
        setup:
            Network net = new Network("192.168.1.10", 24).reset()
        expect:
            assert net.address.hostAddress == "192.168.1.0"
            assert net.toString() == "192.168.1.0/24"
            assert net.toInteger() == -1062731520
            assert new Network(net.toInteger()).address.hostAddress == net.address.hostAddress
    }
}
