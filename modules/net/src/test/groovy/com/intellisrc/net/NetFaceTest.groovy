package com.intellisrc.net

import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 2022/04/12.
 */
@Ignore // Not available in CI/CD
class NetFaceTest extends Specification {
    def "get first ip4"() {
        setup :
            NetFace face = LocalHost.localNetFace
        expect:
            assert !face.ip4List.empty
            assert face.firstIp4
    }
    def "get first ip6"() {
        setup :
            NetFace face = LocalHost.localNetFace
        expect:
            assert !face.ip6List.empty
            assert face.firstIp6
    }
    def "get the ip which starts with"() {
        setup:
            NetFace face = LocalHost.localNetFace
            Inet4Address ip = face.getIpStarts(face.firstIp4.get().hostAddress.substring(0, 5)).get()
        expect:
            assert ip.hostAddress
        cleanup:
            println ip.hostAddress
    }
    def "get local addresses from interface"() {
        setup:
            List<Inet4Address> addresses = LocalHost.localNetFace.localAddresses
        expect:
            assert !addresses.empty
        cleanup:
            addresses.each {
                println it.hostAddress
            }
    }
    @Ignore // It may fail as it requires connection
    def "check if interface is connected"() {
        expect:
            assert LocalHost.localNetFace.isConnected()
    }
}
