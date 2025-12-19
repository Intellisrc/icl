package com.intellisrc.web.protocols

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http2 extends Http {
    int idleTimeout = Millis.MIN_5

    Http2(WebService server) {
        super(server)
    }

    @Override
    AbstractNetworkConnector prepareConnector() {
        assert server : "No server specified"

        HttpConfiguration httpConfig = createHttpConfiguration()

        ServerConnector connector
        HttpConnectionFactory h1

        if (server.secure) {
            // HTTPS config MUST include SecureRequestCustomizer
            httpConfig.addCustomizer(new SecureRequestCustomizer())

            h1 = new HttpConnectionFactory(httpConfig)

            SslContextFactory.Server sslContextFactory = getSSLContextFactory()

            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig)
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("h2", "http/1.1")
            alpn.setDefaultProtocol("http/1.1")

            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.protocol)

            connector = new ServerConnector(
                server.server,
                ssl,
                alpn,
                h2,
                h1
            )
        } else {
            Log.w("Unsecure HTTP/2 will not work in most browsers. Enable HTTPS to fix it.")

            h1 = new HttpConnectionFactory(httpConfig)
            HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig)
            connector = new ServerConnector(server.server, h1, h2c)
        }

        connector.setIdleTimeout(idleTimeout)
        return connector
    }
}
