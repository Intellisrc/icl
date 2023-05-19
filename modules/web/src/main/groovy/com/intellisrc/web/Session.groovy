package com.intellisrc.web

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.session.Session as HTTPSession
import org.eclipse.jetty.websocket.api.Session as WebsocketSession

/**
 * @since 17/04/21.
 */
@CompileStatic
class Session {
    WebsocketSession websocketSession
    HTTPSession httpSession
    InetAddress address
    String userID
    Map data
    /**
     * Backward compatibility with Spark
     * @return
     */
    String id() {
        return id
    }
    String getId() {
        return httpSession.id //TODO
    }
    boolean getNew() {
        return httpSession.new
    }
    String attribute(String name) {
        return "" //TODO
    }
    /**
     * Set an attribute value
     * @param name
     * @param value
     */
    void attribute(String name, Object value) {
        //TODO
    }
    boolean invalidate() {
        return true //TODO
    }
}