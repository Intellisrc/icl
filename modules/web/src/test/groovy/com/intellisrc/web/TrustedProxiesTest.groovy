package com.intellisrc.web

import com.intellisrc.etc.JSON
import com.intellisrc.net.LocalHost
import com.intellisrc.net.Network
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.TrustedProxies
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Tests that the X-Forwarded-For header is only trusted
 * when the connection comes from a trusted proxy
 *
 * @since 2026/09/11.
 */
class TrustedProxiesTest extends Specification {

    def "TrustedProxies accepts Strings, InetAddress, Network and collections"() {
        when:
            TrustedProxies proxies = new TrustedProxies([
                "10.0.0.1",                          // exact IP
                "192.168.0.0/24",                    // CIDR
                new Network("10.1.0.0/16"),          // Network object
                "172.16.0.1".toInetAddress(),        // InetAddress
                ["10.2.0.1"]                         // nested collection
            ])
        then:
            !proxies.isEmpty()
            proxies.isTrusted("10.0.0.1")
            proxies.isTrusted("10.0.0.1:12345") // ports are ignored
            !proxies.isTrusted("10.0.0.2")
            proxies.isTrusted("192.168.0.55")
            !proxies.isTrusted("192.169.0.1")
            proxies.isTrusted("10.1.234.5")
            proxies.isTrusted("172.16.0.1")
            proxies.isTrusted("10.2.0.1")
            !proxies.isTrusted("10.3.0.1")
            !proxies.isTrusted("unknown")
            !proxies.isTrusted("")
            !proxies.isTrusted(null)
    }

    def "TrustedProxies accepts IP ranges"() {
        when:
            TrustedProxies proxies = new TrustedProxies(["10.0.0.1-10"])
        then:
            proxies.isTrusted("10.0.0.1")
            proxies.isTrusted("10.0.0.10")
            !proxies.isTrusted("10.0.0.11")
    }

    def "Empty list of proxies trusts nothing"() {
        expect:
            new TrustedProxies([]).isEmpty()
            new TrustedProxies(null).isEmpty()
    }

    def "cleanIp removes ports and brackets"() {
        expect:
            TrustedProxies.cleanIp(" 10.0.0.1 ") == "10.0.0.1"
            TrustedProxies.cleanIp("10.0.0.1:1234") == "10.0.0.1"
            TrustedProxies.cleanIp("[::1]:1234") == "::1"
            TrustedProxies.cleanIp("::1") == "::1"
    }

    private Service ipService() {
        return new Service(
            path: "ip",
            action: { Request request ->
                return [ip: request.ip()]
            }
        )
    }

    /**
     * Request /ip, optionally sending an X-Forwarded-For header
     */
    private String ip(int port, String forwarded = null) {
        HttpRequest.Builder builder = HttpRequest.newBuilder("http://127.0.0.1:${port}/ip".toURI()).GET()
        if (forwarded != null) {
            builder.header("X-Forwarded-For", forwarded)
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString())
        assert response.statusCode() == 200 : response.body()
        return JSON.decode(response.body()).ip?.toString()
    }

    def "X-Forwarded-For is ignored when no proxy is trusted"() {
        setup:
            int port = LocalHost.freePort
            WebService web = new WebService(port: port)
            web.add(ipService())
            web.start(true)
        expect: "the connection IP is always returned"
            ip(port) == "127.0.0.1"
            ip(port, "1.2.3.4") == "127.0.0.1" // forged header is ignored
            ip(port, "1.2.3.4, 10.0.0.1") == "127.0.0.1"
        cleanup:
            web?.stop()
    }

    def "X-Forwarded-For is used when the connection comes from a trusted proxy"() {
        setup:
            int port = LocalHost.freePort
            WebService web = new WebService(
                port: port,
                trustedProxies: ["127.0.0.1", "10.0.0.0/8"]
            )
            web.add(ipService())
            web.start(true)
        expect:
            ip(port, "1.2.3.4") == "1.2.3.4"
            ip(port, "1.2.3.4:9999") == "1.2.3.4"
            // the client is the right-most IP which is not a trusted proxy:
            ip(port, "9.9.9.9, 10.0.0.1") == "9.9.9.9"
            // "8.8.8.8" is claimed by the untrusted "9.9.9.9", so it can't be trusted:
            ip(port, "8.8.8.8, 9.9.9.9, 10.0.0.1") == "9.9.9.9"
            // when the whole chain is trusted, the left-most IP is the client:
            ip(port, "8.8.8.8, 10.0.0.2, 10.0.0.1") == "8.8.8.8"
            // without the header, the connection IP is returned:
            ip(port) == "127.0.0.1"
        cleanup:
            web?.stop()
    }

    def "All IPs in the chain trusted returns the first one"() {
        setup:
            int port = LocalHost.freePort
            WebService web = new WebService(port: port, trustedProxies: ["127.0.0.1", "10.0.0.0/8"])
            web.add(ipService())
            web.start(true)
        expect:
            ip(port, "10.0.0.1, 10.0.0.2") == "10.0.0.1"
        cleanup:
            web?.stop()
    }

    def "trustedProxies accepts Network and InetAddress objects"() {
        setup:
            int port = LocalHost.freePort
            WebService web = new WebService(
                port: port,
                trustedProxies: [new Network("127.0.0.0/8"), "127.0.0.1".toInetAddress()]
            )
            web.add(ipService())
            web.start(true)
        expect:
            ip(port, "4.4.4.4") == "4.4.4.4"
        cleanup:
            web?.stop()
    }

    def "web.trusted.proxies can be set from configuration"() {
        setup:
            System.setProperty("web.trusted.proxies", "[127.0.0.1]")
            int port = LocalHost.freePort
            WebService web = new WebService(port: port)
            web.add(ipService())
            web.start(true)
        expect:
            ip(port, "5.6.7.8") == "5.6.7.8"
        cleanup:
            System.clearProperty("web.trusted.proxies")
            web?.stop()
    }
}
