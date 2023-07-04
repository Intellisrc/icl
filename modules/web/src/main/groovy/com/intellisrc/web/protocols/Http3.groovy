package com.intellisrc.web.protocols


import com.intellisrc.web.WebService
import groovy.transform.CompileStatic

/*
import org.eclipse.jetty.http3.api.Session
import org.eclipse.jetty.http3.server.HTTP3ServerConnector
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory
import org.eclipse.jetty.server.AbstractNetworkConnector
*/
/**
 * FIXME: Currently it is not possible to launch a HTTP/3 enabled server as explained here:
 * https://stackoverflow.com/questions/76337210/http-3-server-using-jetty-11-not-responding/76348759
 * 
 * @since 2023/05/19.
 */
@CompileStatic
@Deprecated // Not ready
class Http3 extends Http {
    static final boolean ready = false
    Http3(WebService server) {
        super(server)
        assert ready : "HTTP/3 Support is not ready"
    }
/*
    @Override
    AbstractNetworkConnector prepareConnector() {
        assert server : "No server specified"
        assert server.secure : "HTTPS is required in order to use HTTP/3"
        Log.w("HTTP/3 is experimental and will most probably fail to work.")
        // HTTP(S) Configuration
        /*
        HttpConnectionFactory h1 = getConnectionFactory(createHttpConfiguration())
        SslContextFactory.Server sslContextFactory = getSSLContextFactory()
        HTTP3ServerConnectionFactory h3 = new HTTP3ServerConnectionFactory(h1.httpConfiguration)
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server.server, sslContextFactory, h3)
         *
        Session.Server.Listener sessionListener = new Session.Server.Listener(){}
        RawHTTP3ServerConnectionFactory http3 = new RawHTTP3ServerConnectionFactory(sessionListener)
        http3.getHTTP3Configuration().setStreamIdleTimeout(15000)

        // Create and configure the HTTP3ServerConnector.
        HTTP3ServerConnector connector = new HTTP3ServerConnector(server.server, getSSLContextFactory(), http3)
        // Configure the max number of requests per QUIC connection.
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024)
        return connector
    }
 */
}
