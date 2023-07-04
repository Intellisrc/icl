package com.intellisrc.web.service

import com.intellisrc.etc.Mime
import groovy.transform.CompileStatic

import static org.eclipse.jetty.http.HttpMethod.GET

/**
 * This is the class that you need to use as interface
 * in order to provide SSE service
 * @since 2023/06/30.
 */
@CompileStatic
trait ServiciableSentEvents implements ServiciableMultiple {
    /**
     * Broadcast service to send ServerSentEvents
     * TODO: document
     */
    ServerSendBroadcastService sse = new ServerSendBroadcastService()

    List<Service> getServices() {
        return [
            new Service(
                method: GET,
                contentType: Mime.SSE,
                compress: false,
                action: {
                    Request req, Response res ->
                        sse.doGet(req, res)
                        return "event: open"
                }
            )
        ]
    }
}
