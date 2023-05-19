package com.intellisrc.web.protocols

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * @since 2023/05/19.
 */
@CompileStatic
interface Protocolable {
    boolean secure = false
    void init()
    ServerConnector getConnector()
    SslContextFactory.Server getContextFactory()
}