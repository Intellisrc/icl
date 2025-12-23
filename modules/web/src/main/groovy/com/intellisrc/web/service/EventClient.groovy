package com.intellisrc.web.service

import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.websocket.Session as JakartaSession

/**
 * Client used in WebSockets and ServerSendEvents
 * @since 2023/07/05.
 */
@CompileStatic
class EventClient {
    @SuppressWarnings('GrFinalVariableAccess')
    protected final AsyncContext context
    protected final int maxSize

    final InetAddress ip
    final String id
    Session session

    EventClient(HttpServletRequest request, String id, long timeout, int maxSize, JakartaSession wsSession = null) {
        ip = request.remoteAddr.toInetAddress()
        this.id = id
        this.maxSize = maxSize
        this.session = request?.session ? new Session(id, request.session) : new Session(id, wsSession)
        if(! this.session.websocketSession && wsSession) {
            this.session.websocketSession = wsSession
        }
        if(request.asyncSupported &&! request.asyncStarted) {
            try {
                context = request.startAsync()
                context.setTimeout(timeout)
            } catch (Exception ignore) {
                // Async failed
            }
        }
    }
}
