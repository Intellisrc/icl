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

    @Override
    void onOpen(Session jakartaSession, EndpointConfig config) {
        Request request = (Request) config.userProperties["request"]
        request.setWebSocket(jakartaSession)

        service = (WebSocketBroadcastService) config.userProperties["service"]

        String id = service.identifier.call(request)

        EventClient client = new EventClient(
            request,
            id,
            service.timeout,
            service.maxSize
        )

        service.clientList << client
        service.onClientConnect.call(client)
        service.onClientListUpdated.call(service.clientList.toList())

        jakartaSession.addMessageHandler(String, {
            String message ->
                onMessage(jakartaSession, message)
        } as MessageHandler.Whole<String>)
        Log.d("Client: %s (%s) connected", client.id, jakartaSession.id)
    }

    Optional<EventClient> getClient(Session jakartaSession) {
        return Optional.ofNullable(service.clientList.find { it.session.id == jakartaSession.id })
    }

    void onMessage(Session session, String message) {
        if (CLOSE_MESSAGES.contains(message.toLowerCase())) {
            onClose(session, new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, String.format("'%s' message received.", message.toLowerCase())))
        } else {
            Optional<EventClient> optionalEventClient = getClient(session)
            if(optionalEventClient.present) {
                service.onMessageReceived.call(optionalEventClient.get(), new WebMessage(message))
            } else {
                Log.w("Client: %s not found in client list", session.id)
            }
        }
    }

    @Override
    void onClose(Session session, CloseReason reason) {
        String reasonText = reason.reasonPhrase
        if(!reasonText && reason.closeCode.code) {
            reasonText = CloseReason.CloseCodes.getCloseCode(reason.closeCode.code).toString()
        }
        Log.d("Client [%s] disconnected. [%d] Reason: %s", session.id, reason.closeCode.code, reasonText)
        Optional<EventClient> optionalEventClient = getClient(session)
        if(optionalEventClient.present) {
            service.clientList.remove(optionalEventClient.get())
            service.onClientDisconnect.call(optionalEventClient.get())
        } else {
            Log.w("Client: %s not found in client list", session.id)
        }
    }

    @Override
    void onError(Session session, Throwable cause) {
        Log.w("WebSocket error (Client: %s)", cause, session.id)
    }
}