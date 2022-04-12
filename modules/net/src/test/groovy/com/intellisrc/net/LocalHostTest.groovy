package com.intellisrc.net

import spock.lang.Ignore
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
    def "get Host name"() {
        setup:
            println LocalHost.name
        expect:
            assert LocalHost.name != ""
    }
    def "get interfaces"() {
        setup:
            List<NetworkInterface> ifaces = LocalHost.interfaces
            ifaces.each {
                println it.name
            }
        expect:
            assert ! ifaces.empty
            assert ifaces.first().name
    }
    def "get netfaces"() {
        setup:
            List<NetFace> faces = LocalHost.netFaces
            faces.each {
                println it.name + " >> " + it.mac.toString()
            }
        expect:
            assert ! faces.empty
            assert ! faces.first().mac.toString().empty
    }
    def "get network interface for IP"() {
        setup:
            InetAddress ip = LocalHost.ip4Addresses.first()
            def optInet = LocalHost.getNetworkInterface(ip)
        expect:
            assert optInet.present
            assert optInet.get().name
        cleanup:
            println optInet.get().name + " >>" + ip.hostAddress
    }
    def "get netface for ip"() {
        setup:
            InetAddress ip = LocalHost.ip4Addresses.first()
            def optFace = LocalHost.getNetFace(ip)
        expect:
            assert optFace.present
            assert optFace.get().name
            assert optFace.get().mac
        cleanup:
            println optFace.get().name + " >> " + ip.hostAddress + " >> " + optFace.get().mac.toString()
    }
    def "get IP4 addresses"() {
        setup:
            def addresses = LocalHost.ip4Addresses
        expect:
            assert ! addresses.empty
        cleanup:
            addresses.each { println it.hostAddress }
    }
    def "get local network IP4"() {
        setup:
            Inet4Address ip = LocalHost.localNetworkIp4
        expect:
            assert ip.hostAddress
        cleanup:
            println ip.hostAddress
    }
    def "get IP6 addresses"() {
        setup:
            def addresses = LocalHost.ip6Addresses
        expect:
            assert ! addresses.empty
        cleanup:
            addresses.each { println it.hostAddress }
    }
    @Ignore // Requires open port
    def "check if port is open"() {
        expect:
            assert LocalHost.hasOpenPort(80)
    }
    def "get Local Network"() {
        expect:
            assert LocalHost.localNetwork.cidr.contains(".0/")
        cleanup:
            println LocalHost.localNetwork.cidr
    }
}