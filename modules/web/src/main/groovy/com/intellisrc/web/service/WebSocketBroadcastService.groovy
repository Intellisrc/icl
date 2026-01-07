package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic
import jakarta.servlet.ServletContext
import jakarta.websocket.SendHandler
import jakarta.websocket.SendResult
import jakarta.websocket.Session as JakartaSession
import jakarta.websocket.server.ServerContainer
import jakarta.websocket.server.ServerEndpointConfig
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer

@CompileStatic
class WebSocketBroadcastService implements BroadcastService {
    int maxSize = Config.any.get("web.ws.max.size", 64) // KB
    String path = "/"
    ServletContextHandler contextHandler

    void configure(ServletContextHandler context) {
        contextHandler = context
        JakartaWebSocketServletContainerInitializer.configure(context, {
            ServletContext sc, ServerContainer container ->
                container.defaultMaxTextMessageBufferSize = maxSize * 1024
                container.defaultMaxBinaryMessageBufferSize = maxSize * 1024
                container.defaultMaxSessionIdleTimeout =
                    timeout * Millis.SECOND

                container.addEndpoint(
                    ServerEndpointConfig.Builder
                        .create(EventEndPoint, path)
                        .configurator(new EventEndPoint.Configurator(this))
                        .build()
                )
        })
    }

    /* ------------------------------------------------------------ */
    /* Message sending                                              */
    /* ------------------------------------------------------------ */

    @Override
    void sendTo(
        EventClient client,
        WebMessage message,
        SuccessCallback onSuccess = null,
        FailCallback onFail = null
    ) {
        JakartaSession session = client?.session?.websocketSession

        if (session && session.isOpen()) {
            session.asyncRemote.sendText(message.toString(), {
                SendResult result ->
                if (result.exception) {
                    onFail?.call(result.exception)
                } else {
                    onSuccess?.call()
                }
            } as SendHandler)
        } else {
            Exception e = new IllegalStateException("WebSocket session is not open")
            Log.v("Unable to send message to client: %s", client?.id)
            onFail?.call(e)
        }
    }
}
