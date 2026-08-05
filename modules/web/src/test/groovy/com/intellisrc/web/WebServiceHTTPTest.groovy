package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.crypt.KeyStoreGenerator
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

    /**
     * Perform a GET, optionally with a Range header, returning the raw-byte response.
     */
    HttpResponse<byte[]> requestRange(URI url, String rangeHeader) {
        HttpClient client = HttpClient.newHttpClient()
        HttpRequest.Builder builder = HttpRequest.newBuilder(url).GET()
        if (rangeHeader != null) {
            builder.header("Range", rangeHeader)
        }
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
        Log.i("Status: %d (Range: %s) len=%d", response.statusCode(), rangeHeader, response.body().length)
        return response
    }

    def setup() {
        KeyStoreGenerator ksg = new KeyStoreGenerator(includeIp: true)
        ksg.create(storeFile, "password".toCharArray())
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

    def "File service supports HTTP Range (206 / 416 / fallback 200)"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            // Known content: 250 bytes where byte[i] = i. Named .mp4 so Mime -> video/mp4
            // -> Type.BINARY (a .bin would resolve to application/octet-stream -> Type.STREAM,
            // hitting the SSE branch). Mirrors vidi's MediaService returning a File.
            int size = 250
            byte[] data = new byte[size]
            for (int i = 0; i < size; i++) { data[i] = (byte) i }
            java.io.File media = java.io.File.createTempFile("range", ".mp4")
            media.deleteOnExit()
            media.withOutputStream { it.write(data) }
            def web = new WebService(
                compress: false,
                protocol: HTTP,
                checkSNIHostname: false,
                port: port,
            )
            web.add(new Service(
                path: "media",
                action: { media }   // returns a File (like vidi MediaService)
            ))
        when:
            web.start(true, { conds.evaluate { assert true } })
            conds.await()
            URI base = "http://localhost:${port}/media".toURI()
            HttpResponse<byte[]> closed  = requestRange(base, "bytes=0-99")
            HttpResponse<byte[]> suffix  = requestRange(base, "bytes=-50")
            HttpResponse<byte[]> open    = requestRange(base, "bytes=100-")
            HttpResponse<byte[]> unsat   = requestRange(base, "bytes=300-400")
            HttpResponse<byte[]> multi   = requestRange(base, "bytes=0-10,20-30")
            HttpResponse<byte[]> noRange = requestRange(base, null)
        then:
            web.isRunning()
            // closed range -> 206, first 100 bytes
            closed.statusCode() == 206
            closed.headers().firstValue("Content-Range").orElse("") == "bytes 0-99/${size}".toString()
            closed.headers().firstValue("Content-Length").orElse("") == "100"
            closed.headers().firstValue("Accept-Ranges").orElse("") == "bytes"
            java.util.Arrays.equals(closed.body(), java.util.Arrays.copyOfRange(data, 0, 100))
            // suffix -> last 50 bytes
            suffix.statusCode() == 206
            suffix.headers().firstValue("Content-Range").orElse("") == "bytes 200-249/${size}".toString()
            java.util.Arrays.equals(suffix.body(), java.util.Arrays.copyOfRange(data, 200, 250))
            // open range -> 206, from 100 to end (150 bytes)
            open.statusCode() == 206
            open.headers().firstValue("Content-Range").orElse("") == "bytes 100-249/${size}".toString()
            java.util.Arrays.equals(open.body(), java.util.Arrays.copyOfRange(data, 100, 250))
            // unsatisfiable -> 416
            unsat.statusCode() == 416
            unsat.headers().firstValue("Content-Range").orElse("") == "bytes */${size}".toString()
            // multi-range -> fall back to full 200
            multi.statusCode() == 200
            java.util.Arrays.equals(multi.body(), data)
            // no range -> full 200
            noRange.statusCode() == 200
            java.util.Arrays.equals(noRange.body(), data)
        cleanup:
            web.stop()
            media.delete()
    }

    def "File image (.png) is served over HTTP (regression: must not cast/buffer)"() {
        setup:
            def conds = new AsyncConditions()
            int port = LocalHost.freePort
            byte[] hdr = [-119, 80, 78, 71, 13, 10, 26, 10] as byte[]
            byte[] png = new byte[hdr.length + 64]
            System.arraycopy(hdr, 0, png, 0, hdr.length)
            java.io.File img = java.io.File.createTempFile("pic", ".png")
            img.deleteOnExit()
            img.withOutputStream { it.write(png) }
            def web = new WebService(
                compress: false,
                protocol: HTTP,
                checkSNIHostname: false,
                port: port,
            )
            web.add(new Service(path: "pic", action: { img }))
        when:
            web.start(true, { conds.evaluate { assert true } })
            conds.await()
            HttpResponse<byte[]> resp = requestRange("http://localhost:${port}/pic".toURI(), null)
        then:
            web.isRunning()
            resp.statusCode() == 200
            resp.headers().firstValue("Content-Type").orElse("").startsWith("image/png")
            java.util.Arrays.equals(resp.body(), png)
        cleanup:
            web.stop()
            img.delete()
    }
}
