package com.intellisrc.web.protocols

import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.http3.api.Session
import org.eclipse.jetty.http3.server.HTTP3ServerConnector
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory
import org.eclipse.jetty.server.AbstractNetworkConnector

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http3 extends Http {
    Http3(WebService server) {
        super(server)
    }

    @Override
    AbstractNetworkConnector prepareConnector() {
        assert server : "No server specified"
        assert server.secure : "HTTPS is required in order to use HTTP/3"
        // HTTP(S) Configuration
        /*
        HttpConnectionFactory h1 = getConnectionFactory(createHttpConfiguration())
        SslContextFactory.Server sslContextFactory = getSSLContextFactory()
        HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(h1.httpConfiguration)
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server.server, sslContextFactory, h3)
         */
        Session.Server.Listener sessionListener = new Session.Server.Listener(){}
        RawHTTP3ServerConnectionFactory http3 = new RawHTTP3ServerConnectionFactory(sessionListener)
        http3.getHTTP3Configuration().setStreamIdleTimeout(15000)

        // Create and configure the HTTP3ServerConnector.
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server.server, getSSLContextFactory(), http3)
        // Configure the max number of requests per QUIC connection.
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024)
        return connector
    }
}
