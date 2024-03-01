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
 * Extension of JettyWebSocketServlet.
 * @since 2023/07/04.
 */
@CompileStatic
class WebSocketBroadcastService extends JettyWebSocketServlet implements BroadcastService {
    int maxSize = Config.any.get("web.ws.max.size", 64) // KB
    String path = "/"

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
        List<String> closeMessages = Config.any.get("websocket.close.list", ["quit","exit","close","bye"]) //TODO: document
        @Override
        void onWebSocketConnect(JettySession sess) {
            super.onWebSocketConnect(sess)
            Log.i("[%s] Client connected: %s", request.remoteAddr, id)
            EventClient client = new EventClient(request, id, timeout, maxSize, sess)
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
            Log.w("WebSocket error: %s", cause)
        }
    }

    /**
     * Use this method when you don't care about success or failure status
     * @param client
     * @param message
     */
    void sendTo(EventClient client, WebMessage message) {
        sendTo(client, message, {}, {
            Throwable t ->
                Log.v("Unable to send message to: %s", client.id)
        })
    }
    /**
     * Use this method if you want to handle only the success case (failure will be ignored)
     * @param client
     * @param message
     * @param onSuccess
     */
    void sendTo(EventClient client, WebMessage message, SuccessCallback onSuccess) {
        sendTo(client, message, onSuccess, { Throwable t -> })
    }
    /**
     * Send Message to client
     * @param client
     * @param message
     * @param onSuccess
     * @param onFail
     */
    @Override
    void sendTo(EventClient client, WebMessage message, SuccessCallback onSuccess, FailCallback onFail) {
        if(client.session) {
            client.session.websocketSession.remote.sendString(message.toString())
        } else {
            Log.w("Session was empty")
            onFail?.call(new Exception("Session was empty"))
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
