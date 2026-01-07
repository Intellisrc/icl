package com.intellisrc.web.protocols

import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.server.AbstractNetworkConnector
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
abstract class HttpProtocol {
    final WebService server
    boolean trustForwardHeaders = true
    boolean checkSNIHostname = true
    boolean sniRequired = false

    HttpProtocol(WebService server) {
        this.server = server
    }

    Connector getConnector() {
        return setup(prepareConnector())
    }

    protected Connector setup(AbstractNetworkConnector connector) {
        connector.setIdleTimeout(server.timeout)
        connector.setHost(server.address.hostAddress)
        connector.setPort(server.port)
        return connector
    }

    abstract void init()
    abstract protected AbstractNetworkConnector prepareConnector()
    abstract protected SslContextFactory.Server getSSLContextFactory()
    abstract protected HttpConnectionFactory getConnectionFactory(HttpConfiguration httpConfiguration)
}