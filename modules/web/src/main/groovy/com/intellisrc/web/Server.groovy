package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.web.protocols.Protocol
import com.intellisrc.web.protocols.Protocolable
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.eclipse.jetty.server.Server as JettyServer
import org.eclipse.jetty.util.thread.QueuedThreadPool

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Server {
    @TupleConstructor
    static protected class RouteDefinition {
        Method method
        String path
        String allowType
        Route action
    }

    final int port
    final int threads
    boolean secure = false
    protected String staticPath = ""
    protected int expirationSec = 0
    final Protocolable protocol
    final JettyServer jettyServer
    final ConcurrentLinkedQueue<RouteDefinition> definitions = new ConcurrentLinkedQueue<>()

    Server(int port, Protocol protocol, int threads) {
        this.port = port
        this.threads = threads
        this.protocol = protocol.init()
        jettyServer = threads > 0 ? new JettyServer(new QueuedThreadPool(10, 2, 10000)) : new JettyServer()
        //TODO
    }

    void setStaticPath(String path, int expirationSec) {
        this.staticPath = path
        this.expirationSec = expirationSec
        //TODO
    }

    void start() {
        jettyServer.start()
        if(threads > 0) {
            jettyServer.join() //TODO: check
        }
    }
    void stop() {
        //TODO
    }

    boolean add(Service service, String path, Route route) {
        //TODO: detect possible conflicts
        return definitions.add(new RouteDefinition(service.method, path, service.allowType, route))
    }

    boolean add(RouteDefinition definition) {
        return definitions.add(definition)
    }

    void setKeyStore(KeyStore keyStore) {
        //TODO:
        Log.i("%s : %s", keyStore.file, keyStore.password)
        secure = true
    }
}
