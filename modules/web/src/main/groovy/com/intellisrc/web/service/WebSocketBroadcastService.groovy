package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.websocket.api.Session as JettySession
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

import java.time.Duration

/**
 * @since 2023/07/04.
 */
@CompileStatic
class WebSocketBroadcastService extends JettyWebSocketServlet implements BroadcastService {
    int maxSize = Config.get("web.ws.max.size", 64) // KB
    String path = "/"
    final MessageHandler handler

    WebSocketBroadcastService(MessageHandler handler, String path) {
        this.handler = handler
        this.path = path
    }

    /**
     * This class is applied to a single client
     */
    class EventEndpoint extends WebSocketAdapter {
        final HttpServletRequest request
        final String id
        EventEndpoint(HttpServletRequest request) {
            this.request = request
            Request req = new Request(request)
            id = identifier.call(req)
        }
        List<String> closeMessages = Config.get("websocket.close.list", ["quit","exit","close","bye"]) //TODO: document
        @Override
        void onWebSocketConnect(JettySession sess) {
            super.onWebSocketConnect(sess)
            Log.i("[%s] Client connected: %s", request.remoteAddr, id)
            EventClient client = new EventClient(request, id, timeout, maxSize)
            clientList << client
            onClientConnect.call(client)
            onClientListUpdated.call(clientList.toList())
        }

        @Override
        void onWebSocketText(String message) {
            super.onWebSocketText(message)
            if (closeMessages.contains(message.toLowerCase())) {
                Log.i("Client [%s] disconnected", id)
                Optional<EventClient> clientOpt = get(id)
                if(clientOpt.present) {
                    EventClient client = clientOpt.get()
                    disconnectClient(client)
                }
                getSession().close(StatusCode.NORMAL, "client request")
            } else {
                Log.v("Received TEXT message: %s", message)
                Optional<EventClient> clientOpt = get(id)
                WebMessage msg = new WebMessage(message)
                if(clientOpt.present) {
                    onMessageReceived.call(clientOpt.get(), msg)
                }
                WebMessage reply = handler.call(msg)
                if(reply) {
                    if(clientOpt.present) {
                        sendTo(clientOpt.get(), reply, {
                            Log.v("[%s] Sent reply: ", id, reply.toString())
                        }, {
                            Throwable t ->
                                Log.w("Unable to send reply to: %s", id)
                        })
                    }
                }
            }
        }

        @Override
        void onWebSocketClose(int statusCode, String reason) {
            if(statusCode != StatusCode.NORMAL) {
                Optional<EventClient> clientOpt = get(id)
                if(clientOpt.present) {
                    disconnectClient(clientOpt.get())
                }
            }
            super.onWebSocketClose(statusCode, reason)
            Log.v("Socket Closed: [%d] %s", statusCode, reason)
        }

        @Override
        void onWebSocketError(Throwable cause) {
            super.onWebSocketError(cause)
            Log.w("WebSocket error: %s", cause.message ?: cause.cause)
        }
    }

    @Override
    void sendTo(EventClient client, WebMessage message, SuccessCallback onSuccess, FailCallback onFail) {
        if(client) {
            sendTo(client, message, {
                Log.v("[%s] Sent message: ", client.id, message.toString())
            }, {
                Throwable t ->
                    Log.w("Unable to send reply to: %s", client.id)
            })
        }
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.maxTextMessageSize =
            factory.maxBinaryMessageSize =
                factory.inputBufferSize = maxSize * 1024
        factory.addMapping(path, {
            // EndPoint creator:
            JettyServerUpgradeRequest request, JettyServerUpgradeResponse response ->
               new EventEndpoint(request.httpServletRequest)
        })
        factory.idleTimeout = Duration.ofMillis(timeout * Millis.SECOND)
    }
}
