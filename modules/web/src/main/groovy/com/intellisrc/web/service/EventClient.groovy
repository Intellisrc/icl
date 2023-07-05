package com.intellisrc.web.service

import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.HttpServletRequest

/**
 * Client used in WebSockets and ServerSendEvents
 * @since 2023/07/05.
 */
@CompileStatic
class EventClient {
    protected final AsyncContext context
    final InetAddress ip
    final String id
    protected final int maxSize

    EventClient(HttpServletRequest request, String id, long timeout, int maxSize) {
        ip = request.remoteAddr.toInetAddress()
        this.id = id
        this.maxSize = maxSize
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
