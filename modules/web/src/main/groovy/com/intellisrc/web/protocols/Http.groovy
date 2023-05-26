package com.intellisrc.web.protocols

import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import org.conscrypt.OpenSSLProvider
import org.eclipse.jetty.server.*
import org.eclipse.jetty.util.ssl.SslContextFactory

import java.security.Security

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http extends HttpProtocol {
    Http(WebService server) {
        super(server)
    }

    @Override
    void init() {
        if(server.secure) {
            Security.insertProviderAt(new OpenSSLProvider(), 1)
            //Security.insertProviderAt(Conscrypt.newProviderBuilder().provideTrustManager(false).build(), 1)
        }
    }

    @Override
    AbstractNetworkConnector prepareConnector() {
        assert server : "Server was not specified"
        return new ServerConnector(server.server, getSSLContextFactory(), getConnectionFactory(createHttpConfiguration()))
    }

    @Override
    SslContextFactory.Server getSSLContextFactory() {
        SslContextFactory.Server sslContextFactory = null
        if(server.secure) {
            sslContextFactory = new SslContextFactory.Server()
            sslContextFactory.setEndpointIdentificationAlgorithm("https")
            sslContextFactory.setKeyStorePath(server.ssl.file.absolutePath)
            if (server.ssl.password != null) {
                sslContextFactory.setKeyStorePassword(server.ssl.password.toString())
            }
            sslContextFactory.setUseCipherSuitesOrder(true)
        }
        return sslContextFactory
    }

    @Override
    HttpConnectionFactory getConnectionFactory(HttpConfiguration httpConfiguration) {
        if(server.secure) {
            SecureRequestCustomizer src = new SecureRequestCustomizer()
            src.setSniHostCheck(checkSNIHostname)
            src.setSniRequired(sniRequired)
            httpConfiguration.addCustomizer(src)
            httpConfiguration.setSecurePort(server.port)
        }
        return new HttpConnectionFactory(httpConfiguration)
    }

    HttpConfiguration createHttpConfiguration() {
        HttpConfiguration httpConfig = new HttpConfiguration()
        httpConfig.setSecureScheme("https")
        httpConfig.setSendServerVersion(false)
        if(trustForwardHeaders) {
            httpConfig.addCustomizer(new ForwardedRequestCustomizer())
        }
        return httpConfig
    }
}
