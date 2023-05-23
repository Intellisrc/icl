package com.intellisrc.web.protocols

import com.intellisrc.web.Server
import groovy.transform.CompileStatic
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http2 extends Http {
    Http2(Server server) {
        super(server)
    }

    @Override
    ServerConnector prepareConnector() {
        assert server : "No server specified"
        // HTTP(S) Configuration
        HttpConnectionFactory h1 = getConnectionFactory(createHttpConfiguration())
        ServerConnector connector
        if(server.secure) {
            SslContextFactory.Server sslContextFactory = getSSLContextFactory()
            sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR)

            // HTTP2 factory
            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(h1.httpConfiguration)
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory()
            alpn.setDefaultProtocol(h2.getProtocol())
            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol())
            // HTTP2 Connector
            connector = new ServerConnector(server.server, ssl, alpn, h2, h1)
        } else {
            HTTP2CServerConnectionFactory h2 = new HTTP2CServerConnectionFactory(h1.httpConfiguration)
            connector = new ServerConnector(server.server, h2, h1)
        }
        return connector
    }
}
