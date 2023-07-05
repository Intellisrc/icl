package com.intellisrc.web.samples

import com.intellisrc.core.Millis
import com.intellisrc.web.service.*
import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
class ChatService implements ServiciableWebSocket {
    Map<String, String> usersList = [:]

    ChatService() {
        ws.onClientConnect = {
            EventClient client ->
                usersList[client.id] = client.ip.hostAddress
        }
        ws.onClientDisconnect = {
            EventClient client ->
                usersList.remove(client.id)
        }
        ws.timeout = Millis.HOUR
        ws.identifier = {
            Request request ->
                return request.queryParams("user") ?: randomName
        }
    }

    static String getRandomName() {
        return "Guest "+(new Random().nextInt(100) + 1)
    }

    @Override
    MessageHandler getMessageHandler() {
        return {
            WebMessage msg ->
                return new WebMessage(
                    message : "Received",
                    list : usersList.keySet(),
                    type : "txt"
                )
        }
    }

    String getPath() {
        "/ws/chat"
    }
}
