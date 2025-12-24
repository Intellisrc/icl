package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.websocket.*
import jakarta.websocket.Session
import jakarta.websocket.server.HandshakeRequest
import jakarta.websocket.server.ServerEndpointConfig

@CompileStatic
class EventEndPoint extends Endpoint {
    static final List<String> CLOSE_MESSAGES = Config.any.get("websocket.close.list", ["quit", "exit", "close", "bye"])
    Session session
    EventClient client
    WebSocketBroadcastService service

    static class Configurator extends ServerEndpointConfig.Configurator {
        final WebSocketBroadcastService localService

        Configurator(WebSocketBroadcastService wbService) {
            localService = wbService
        }

        @Override
        void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            // Store HTTP info for later
            InetSocketAddress isa = sec.userProperties["jakarta.websocket.endpoint.remoteAddress"] as InetSocketAddress
            HttpSession httpSession = sec.userProperties["jakarta.websocket.server.HttpSession"] as HttpSession
            HttpServletRequest servletRequest = sec.userProperties["jakarta.websocket.server.HttpServletRequest"] as HttpServletRequest

            sec.userProperties["request"] = new Request(request, isa, httpSession, servletRequest)
            sec.userProperties["headers"] = request.headers
            sec.userProperties["service"] = localService
        }
    }

    @OnOpen
    void onOpen(Session jakartaSession, EndpointConfig config) {
        Request request = (Request) config.userProperties["request"]
        request.setWebSocket(jakartaSession)
        service = (WebSocketBroadcastService) config.userProperties["service"]

        String id = service.identifier.call(request)

        client = new EventClient(
            request,
            id,
            service.timeout,
            service.maxSize
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
}