package com.intellisrc.web.protocols


import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
abstract class HttpProtocol {
    final WebService server
    boolean trustForwardHeaders = true

    HttpProtocol(WebService server) {
        this.server = server
    }

    ServerConnector getConnector() {
        return setup(prepareConnector())
    }

    protected ServerConnector setup(ServerConnector connector) {
        connector.setIdleTimeout(server.timeout)
        connector.setHost(server.address.hostAddress)
        connector.setPort(server.port)
        return connector
    }

    abstract void init()
    abstract protected ServerConnector prepareConnector()
    abstract protected SslContextFactory.Server getSSLContextFactory()
    abstract protected HttpConnectionFactory getConnectionFactory(HttpConfiguration httpConfiguration)
}