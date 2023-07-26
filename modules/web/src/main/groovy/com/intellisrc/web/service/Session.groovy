package com.intellisrc.web.service

import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpSession
import org.eclipse.jetty.websocket.api.Session as JettySession

/**
 * HTTPSession/WebSocket Session wrapper
 * @since 2023/05/25.
 */
@CompileStatic
class Session {
    final String id
    protected HttpSession httpSession = null
    protected JettySession websocketSession = null //This will be set by the WebSocket Server

    Session(String id, HttpSession session) {
        this.id = id
        this.httpSession = session
    }

    Session(String id, JettySession session) {
        this.id = id
        this.websocketSession = session
    }

    Object attribute(String key) {
        return httpSession ? httpSession.getAttribute(key) : ""
    }

    void attribute(String key, Object val) {
        if(val == null) {
            removeAttribute(key)
        } else if(httpSession) {
            httpSession.setAttribute(key, val)
        }
    }

    boolean hasAttribute(String key) {
        return httpSession ? attributes.contains(key) : false
    }

    void removeAttribute(String key) {
        httpSession?.removeAttribute(key)
    }

    Set<String> getAttributes() {
        return httpSession ? httpSession.attributeNames.toSet() : [] as Set<String>
    }

    Map<String, Object> getMap() {
        return attributes.collectEntries { [(it) : attribute(it) ]}
    }

    void invalidate() {
        if(httpSession) {
            httpSession.invalidate()
        }
        if(websocketSession) {
            websocketSession.close()
        }
    }

    boolean isNew() {
        return httpSession ? httpSession.new : false
    }

    boolean isOpen() {
        return websocketSession ? websocketSession.open : false
    }

    HttpSession getHttpSession() {
        return httpSession
    }

    JettySession getWebsocketSession() {
        return websocketSession
    }
}