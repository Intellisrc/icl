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

    EventClient(Request request, String id, long timeout, int maxSize) {
        ip = request.ip.toInetAddress()
        this.id = id
        this.maxSize = maxSize
        this.session = request.session
        if(request.servlet && request.servlet.asyncSupported &&! request.servlet.asyncStarted) {
            try {
                context = request.servlet.startAsync()
                context.setTimeout(timeout)
            } catch (Exception ignore) {
                // Async failed
            }
        }
    }
}
