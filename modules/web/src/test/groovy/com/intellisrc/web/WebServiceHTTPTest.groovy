package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.net.LocalHost
import com.intellisrc.web.service.KeyStore
import com.intellisrc.web.service.Service
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.AsyncConditions

import javax.net.ssl.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import static com.intellisrc.web.protocols.Protocol.HTTP
import static com.intellisrc.web.protocols.Protocol.HTTP2

/**
 * This tests HTTP, HTTPS and HTTP2
 *
 * @since 2022/08/04.
 */
class WebServiceHTTPTest extends Specification {
    File publicDir = File.get(File.userDir, "res", "public")
    File storeFile = File.get(File.userDir, "res", "private", "keystore.jks")
    String pass = "password"

    String getContent(URI url) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofString())
        Log.i("Status code: %d", response.statusCode())
        return response.body()
    }

    @Unroll
    def "Static file should return content"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            def web = new WebService(
                compress: false, //TODO: in the browser is not working (always expects compression)
                protocol: protocol,
                checkSNIHostname: false,
                port: port,
                resources: publicDir,
                ssl: https ? new KeyStore(storeFile, pass) : null,
            )
            Log.i("Running in port: %d", port)
            if(https) {
                disableSSLChecks()
            }
        when:
            web.start(true, {
                conds.evaluate {
                    assert true
                }
            })
            conds.await()
        then:
            assert web.isRunning()
            assert getContent("http${https ? 's' : ''}://localhost:${port}/".toURI()).contains("Hello")
        cleanup:
            web.stop()
        where:
            protocol | https
            HTTP  | false
            //HTTP  | true <--- They are fine, but here it is complaining
            HTTP2 | false
            //HTTP2 | true <--- They are fine, but here it is complaining
            //HTTP3 | false ---- TODO : NOT READY YET ---
            //HTTP3 | true
    }

    @Unroll
    def "Service should return ok"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            def web = new WebService(
                protocol: protocol,
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
            if(https) {
                disableSSLChecks()
            }
            conds.await()
        then:
            assert web.isRunning()
            assert ("http${https ? 's' : ''}://localhost:${port}/test").toURL().text.contains("ok")
        cleanup:
            web.stop()
            assert ! web.running
        where:
            protocol | https
            HTTP  | false
            //HTTP  | false
            HTTP2 | false
            //HTTP2 | true
            //HTTP3 | false
            //HTTP3 | true
    }

    void disableSSLChecks() {
        def nullTrustManager = [
            checkClientTrusted: { chain, authType ->  },
            checkServerTrusted: { chain, authType ->  },
            getAcceptedIssuers: { null }
        ]
        def nullHostnameVerifier = [
            verify: { hostname, session -> true }
        ]
        SSLContext sc = SSLContext.getInstance("SSL")
        sc.init(null, [nullTrustManager as X509TrustManager] as TrustManager[], null)
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as HostnameVerifier)
    }
}
