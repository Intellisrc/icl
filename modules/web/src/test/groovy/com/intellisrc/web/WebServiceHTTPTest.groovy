package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.protocols.Protocol
import com.intellisrc.web.service.KeyStore
import com.intellisrc.web.service.Service
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.AsyncConditions

import javax.net.ssl.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static com.intellisrc.web.protocols.Protocol.HTTP
import static com.intellisrc.web.protocols.Protocol.HTTP2
import static java.net.http.HttpClient.Version.HTTP_1_1
import static java.net.http.HttpClient.Version.HTTP_2

/**
 * This tests HTTP, HTTPS and HTTP2
 *
 * @since 2022/08/04.
 */
class WebServiceHTTPTest extends Specification {
    File publicDir = File.get(File.userDir, "res", "public")
    File storeFile = File.get(File.userDir, "res", "private", "keystore.jks")
    String pass = "password"
    Map<HttpClient.Version, Protocol> versions = [
        (HTTP_1_1) : HTTP,
        (HTTP_2) : HTTP2
        // HTTP_3 doesn't exists yet
    ] as Map<HttpClient.Version, Protocol>

    /**
     * Get the content from an URL and verify that the protocol matches
     * @param url
     * @param protocol
     * @return
     */
    String getContent(URI url, Protocol protocol) {
        HttpClient client
        if(url.scheme == "https") {
            def nullTrustManager = [
                checkClientTrusted: { chain, authType -> },
                checkServerTrusted: { chain, authType -> },
                getAcceptedIssuers: { null }
            ]
            def nullHostnameVerifier = [
                verify: { hostname, session -> true }
            ]
            SSLContext sc = SSLContext.getInstance("SSL")
            sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
            HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
            client = HttpClient.newBuilder().sslContext(sc).build()
        } else {
            client = HttpClient.newHttpClient()
        }
        HttpRequest request = HttpRequest.newBuilder(url).GET().build()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        HttpClient.Version ver = response.version()
        if(versions.containsKey(ver)) {
            assert versions[ver] == protocol : String.format("Protocol doesn't match: %s (%s)", protocol.toString(), url.scheme)
        }
        Log.i("Status code: %d", response.statusCode())
        return response.body()
    }

    @Unroll
    def "Static file should return content"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            def web = new WebService(
                compress: false,
                protocol: protocol,
                checkSNIHostname: false,
                port: port,
                resources: publicDir,
                ssl: https ? new KeyStore(storeFile, pass) : null,
            )
            Log.i("Running in port: %d", port)
        when:
            web.start(true, {
                conds.evaluate {
                    assert true
                }
            })
            conds.await()
        then:
            assert web.isRunning()
            assert getContent("http${https ? 's' : ''}://localhost:${port}/".toURI(), protocol).contains("Hello")
        cleanup:
            web.stop()
        where:
            protocol | https
            HTTP  | false
            HTTP  | true
            HTTP2 | false
            HTTP2 | true
            //HTTP3 | true //<--- not working
    }

    @Unroll
    def "Service should return ok"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            def web = new WebService(
                compress: false,
                protocol: protocol,
                checkSNIHostname: false,
                port: port,
                ssl: https ? new KeyStore(storeFile, pass) : null,
            )
            Log.i("Running in port: %d", port)
            web.add(new Service(
                path: "test",
                action: { "ok" }
            ))
        when:
            web.start(true, {
                conds.evaluate {
                    assert true
                }
            })
            conds.await()
        then:
            assert web.isRunning()
            assert getContent("http${https ? 's' : ''}://localhost:${port}/test".toURI(), protocol).contains("ok")
        cleanup:
            web.stop()
            assert ! web.running
        where:
            protocol | https
            HTTP     | false
            HTTP     | true
            HTTP2    | false
            HTTP2    | true
            //HTTP3 | true
    }
}
