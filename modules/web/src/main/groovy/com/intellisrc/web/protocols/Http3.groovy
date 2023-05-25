package com.intellisrc.web.protocols

import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.http3.server.HTTP3ServerConnectionFactory
import org.eclipse.jetty.http3.server.HTTP3ServerConnector
import org.eclipse.jetty.server.AbstractNetworkConnector
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

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
        // HTTP(S) Configuration
        HttpConnectionFactory h1 = getConnectionFactory(createHttpConfiguration())
        SslContextFactory.Server sslContextFactory = getSSLContextFactory()
        HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(h1.httpConfiguration)
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server.server, sslContextFactory, h3)
        return connector
    }
}
