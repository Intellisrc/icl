package com.intellisrc.web.service


import com.intellisrc.etc.Mime
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import static org.eclipse.jetty.http.HttpMethod.GET

/**
 * This is the class that you need to use as interface
 * in order to provide SSE service
 * @since 2023/06/30.
 */
@CompileStatic
trait ServiciableSentEvents implements ServiciableSingle {
    /**
     * Broadcast service to send ServerSentEvents
     * TODO: document
     */
    ServerSendBroadcastService sse = new ServerSendBroadcastService()

    /**
     * Shortcut to broadcast to all clients
     * @param data
     * @return
     */
    boolean broadcast(Map data) {
        return sse.broadcast(data)
    }
    /**
     * Broadcast with ID
     * @param id
     * @param data
     * @return
     */
    boolean broadcast(int id, Map data) {
        return sse.broadcast(id, data)
    }

    Service getService() {
        return new Service(
            method: GET,
            contentType: Mime.SSE,
            compress: false, //TODO: make it work with true
            action: {
                Request req, Response res ->
                    sse.doGet(req as HttpServletRequest, res as HttpServletResponse)
                    return "event: open"
            }
        )
    }
}
