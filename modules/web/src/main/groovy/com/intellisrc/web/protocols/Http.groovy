package com.intellisrc.web.protocols

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Http implements Protocolable {
    @Override
    void init() {

    }

    @Override
    ServerConnector getConnector() {
        return null
    }

    @Override
    SslContextFactory.Server getContextFactory() {

    }
}
