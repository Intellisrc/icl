package com.intellisrc.web.service

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
trait ServiciableWebSocket extends Serviciable {
    WebSocketBroadcastService ws = new WebSocketBroadcastService(messageHandler, getPath())

    /**
     * MessageHandler takes care of incoming messages and replies
     * @return
     */
    abstract MessageHandler getMessageHandler()

    /**
     * Set WebSocket path
     * @return
     */
    abstract String getPath()

    /**
     * Send Message to client
     * @param userID
     * @param data
     * @return
     */
    boolean sendMessageTo(String userID, final Map data) {
        Optional<EventClient> clientOpt = ws.get(userID)
        boolean sent = false
        if(clientOpt.present) {
            EventClient client = clientOpt.get()
            ws.sendTo(clientOpt.get(), new WebMessage(data), {
                sent = true
            } ,{
                Throwable t ->
                    Log.d("Unable to send message to: %s", client.id)
            })
        }
        return sent
    }

    /**
     * Shortcut to broadcast to all clients
     * @param data
     * @return
     */
    boolean broadcast(Map data) {
        boolean sent = false
        ws.broadcast(new WebMessage(data), {
            sent = true
        }, { Throwable it ->
            Log.w("Unable to send broadcast message: %s", it.message)
        })
        return sent
    }
}
