package com.intellisrc.web.protocols

import com.intellisrc.web.Server
import groovy.transform.CompileStatic
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http3 extends Http {
    Http3(Server server) {
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

            // HTTP3 factory
            HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(h1.httpConfiguration)
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory()
            alpn.setDefaultProtocol(h3.getProtocol())
            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol())
            // HTTP3 Connector
            connector = new ServerConnector(server.server, ssl, alpn, h3, h1)
        } else {
            HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(h1.httpConfiguration)
            connector = new ServerConnector(server.server, h3, h1)
        }
        return connector
    }
}
