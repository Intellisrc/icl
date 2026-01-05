package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.web.service.*
import groovy.transform.CompileStatic

/**
 * This example uses the interface @see ServiciableWebSocket,
 * @see ChatWebSocketService (using 'extend' example)
 * @see WebSocketService (abstract class)
 *
 * WebSocketService extends ServiciableWebSocket, so extending WebSocketService is
 * generally simpler.
 *
 * @since 17/04/19.
 */
@CompileStatic
class ChatWebSocketServiceIface implements ServiciableWebSocket {
    String path = "/chat"
    Map<String, String> usersList = [:]

    WebSocketBroadcastService getWebSocketService() {
        return new WebSocketBroadcastService(
            path: path,
            onClientConnect : {
                EventClient client ->
                    usersList[client.id] = client.ip.hostAddress
                    webSocketService.sendTo(client, new WebMessage(
                        message : "Connected",
                        list : usersList.keySet(),
                        type : "txt"
                    ))
            },
            onClientDisconnect : {
                EventClient client ->
                    usersList.remove(client.id)
                    webSocketService.sendTo(client, new WebMessage(
                        message : "Disconnected",
                        list : usersList.keySet(),
                        type : "txt"
                    ))
            },
            onMessageReceived : {
                EventClient client, WebMessage msg ->
                    Log.i("Server received message: %s", msg.text)
                    webSocketService.sendTo(client, new WebMessage(
                        message : "Received",
                        list : usersList.keySet(),
                        type : "txt"
                    ))
            },
            timeout : Millis.HOUR,
            identifier : {
                Request request ->
                    return request.queryParams("user") ?: randomName
            }

        )
    }

    static String getRandomName() {
        return "Guest "+(new Random().nextInt(100) + 1)
    }
}
