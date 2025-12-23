package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.websocket.*
import jakarta.websocket.Session
import jakarta.websocket.server.ServerEndpointConfig

@CompileStatic
class EventEndPoint extends Endpoint {
    static final List<String> CLOSE_MESSAGES = Config.any.get("websocket.close.list", ["quit", "exit", "close", "bye"])
    Session session
    EventClient client
    WebSocketBroadcastService service

    @OnOpen
    void onOpen(Session jakartaSession, EndpointConfig config) {
        HttpServletRequest request =
            (HttpServletRequest) config.userProperties["request"]

        Request req = new Request(request)
        String id = service.identifier.call(req)

        client = new EventClient(
            request,
            id,
            service.timeout,
            service.maxSize,
            //session.websocketSession
        )

        service.clientList << client
        service.onClientConnect.call(client)
        service.onClientListUpdated.call(service.clientList.toList())
    }

    @OnMessage
    void onMessage(String message) {
        if (CLOSE_MESSAGES.contains(message.toLowerCase())) {
            session.close()
            Log.v("Client disconnected")
            service.disconnectClient(client)
        } else {
            service.onMessageReceived.call(client, new WebMessage(message))
        }
    }

    @OnClose
    void onClose(CloseReason reason) {
        service.disconnectClient(client)
    }

    @OnError
    void onError(Throwable cause) {
        Log.w("WebSocket error", cause)
    }

    /** Inject servlet request */
    static class Configurator extends ServerEndpointConfig.Configurator {
        final WebSocketBroadcastService service

        Configurator(WebSocketBroadcastService service) {
            this.service = service
        }

        @Override
        <T> T getEndpointInstance(Class<T> endpointClass) {
            T ep = endpointClass
                .getDeclaredConstructor()
                .newInstance()

            if (ep instanceof EventEndPoint) {
                ep.service = service
            }

            return ep
        }
    }
}