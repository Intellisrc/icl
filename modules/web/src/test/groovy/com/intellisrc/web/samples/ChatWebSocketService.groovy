package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.web.service.EventClient
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.WebMessage
import com.intellisrc.web.service.WebSocketService
import groovy.transform.CompileStatic

/**
 * Minimal example to handle communication with clients
 * @since 17/04/19.
 */
@CompileStatic
class ChatWebSocketService extends WebSocketService {
    String path = "/chat"

    @Override
    String getIdentifier(Request request) {
        return request.queryParams("user") ?: randomName
    }

    static String getRandomName() {
        return "guest_"+(new Random().nextInt(100) + 1)
    }

    @Override
    WebMessage onClientConnect(EventClient client) {
        Log.i("Client connected")
        return new WebMessage(
            message : "Connected",
            list : clients.collect { it.id },
            type : "txt"
        )
    }
    /* You can override this method if you need the client info:
    WebMessage onMessage(EventClient client, WebMessage msg) {}
     */
    @Override
    WebMessage onMessage(WebMessage msg) {
        Log.i("Server received message: %s", msg.text)
        return new WebMessage(
            message : "Received",
            list : clients.collect { it.id },
            type : "txt"
        )
    }
}
