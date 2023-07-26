package com.intellisrc.web.samples


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

    @Override
    String getIdentifier(Request request) {
        return request.queryParams("user") ?: randomName
    }

    static String getRandomName() {
        return "Guest "+(new Random().nextInt(100) + 1)
    }

    @Override
    WebMessage onMessage(WebMessage msg) {
        return new WebMessage(
            message : "Received",
            list : clients.collect { it.id },
            type : "txt"
        )
    }

    String getPath() {
        "/ws/chat"
    }
}
