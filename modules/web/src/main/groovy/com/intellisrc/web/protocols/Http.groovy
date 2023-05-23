package com.intellisrc.web.protocols

import com.intellisrc.web.Server
import groovy.transform.CompileStatic
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http extends HttpProtocol {
    Http(Server server) {
        super(server)
    }

    @Override
    void init() {

    }

    @Override
    ServerConnector prepareConnector() {
        assert server : "Server was not specified"
        return new ServerConnector(server.server, getConnectionFactory(createHttpConfiguration()))
    }

    @Override
    SslContextFactory.Server getSSLContextFactory() {
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server()
        if(server.secure) {
            sslContextFactory.setKeyStorePath(server.certificate.file.absolutePath)
            if (server.certificate.password != null) {
                sslContextFactory.setKeyStorePassword(server.certificate.password.toString())
            }
            sslContextFactory.setUseCipherSuitesOrder(true)
        }
        return sslContextFactory
    }

    @Override
    HttpConnectionFactory getConnectionFactory(HttpConfiguration httpConfiguration) {
        if(server.secure) {
            httpConfiguration.addCustomizer(new SecureRequestCustomizer())
        }
        return new HttpConnectionFactory(httpConfiguration)
    }

    HttpConfiguration createHttpConfiguration() {
        HttpConfiguration httpConfig = new HttpConfiguration()
        httpConfig.setSecureScheme("https")
        httpConfig.addCustomizer(new SecureRequestCustomizer())
        httpConfig.setSendServerVersion(false)
        if(trustForwardHeaders) {
            httpConfig.addCustomizer(new ForwardedRequestCustomizer())
        }
        return httpConfig
    }
}
