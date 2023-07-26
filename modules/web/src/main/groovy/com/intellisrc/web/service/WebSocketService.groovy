//file:noinspection GrMethodMayBeStatic
package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This class simplifies the code to implement simple communication with clients
 * by implementing @see ServiciableWebSocket, with most of the events inside
 * WebSocketBroadcastService without having to care about sessions.
 * You can use the WebSocketBroadcastService instance as well
 * @since 17/04/19.
 */
@CompileStatic
abstract class WebSocketService implements ServiciableWebSocket {
    final protected ConcurrentLinkedQueue<EventClient> clients = new ConcurrentLinkedQueue<>()
    final protected WebSocketBroadcastService ws = new WebSocketBroadcastService(
        path: path,
        onClientConnect : {
            EventClient client ->
                EventClient existent = getClient(client)
                if(existent) {
                    if(replaceOnDuplicate) {
                        clients.remove(existent)
                        Log.v("Session with the same ID existed. It was replaced.")
                    } else {
                        Log.v("Session with the same ID existed. It was ignored.")
                        return
                    }
                }
                clients << client
                Log.i("Client connected: " + client.id + " (active: %d)", clients.size())
                WebMessage msg = onClientConnect(client)
                if(msg) {
                    ws.sendTo(client, msg)
                }
        },
        onMessageReceived : {
            EventClient client, WebMessage msg ->
                EventClient existent = getClient(client)
                if(existent) {
                    WebMessage reply = onMessage(msg)
                    if(reply) {
                        ws.sendTo(client, reply, {
                            Throwable t ->
                                Log.w("Unable to reply to client: %s", client.id)
                        })
                    }
                } else {
                    Log.w("Message received from nonexistent client: %s", client.id)
                }
        },
        onClientDisconnect : {
            EventClient client ->
                EventClient existent = getClient(client)
                if(existent) {
                    WebMessage msg = onClientDisconnect(client)
                    if(msg) {
                        ws.sendTo(client, msg)
                    }
                    clients.remove(existent)
                }
        },
        timeout : timeout,
        //TODO: check if its the same:
        identifier : { Request it -> it.session().id }
    )
    @Override
    WebSocketBroadcastService getWebSocketService() {
        return ws
    }
    // Override to change (accessed only during initialization):
    protected boolean replaceOnDuplicate = true
    // Override to change (accessed only during initialization):
    protected int timeout = Millis.HOUR

    /**
     * Set WebSocket path
     * @return
     */
    abstract String getPath()
    /**
     * Return a session from ID
     * @param id
     * @return
     */
    protected EventClient getClient(EventClient client) {
        return clients.find { it.id == client.id }
    }
    /**
     * MessageHandler takes care of incoming messages and replies
     * to the same client
     * @return
     */
    WebMessage onMessage(WebMessage msg) { null }
    /**
     * Get Unique identifier. You can override this method.
     * @param client
     * @return
     */
    String getIdentifier(Request request) {
        return request.session()?.id
    }
    /**
     * You may override this method to perform some action each time a client connects
     * @param client
     * @return reply message
     */
    WebMessage onClientConnect(EventClient client) { null }
    /**
     * You may override this method to perform some action before a client disconnects
     * @param client
     * @return reply message
     */
    WebMessage onClientDisconnect(EventClient client) { null }
    /**
     * Send Message to client
     * @param userID
     * @param data
     * @return
     */
    boolean sendTo(String userID, final Map data) {
        Optional<EventClient> clientOpt = ws.get(userID)
        boolean sent = false
        if(clientOpt.present) {
            EventClient client = clientOpt.get()
            ws.sendTo(clientOpt.get(), new WebMessage(data), {
                sent = true
            } ,{
                Throwable t ->
                    Log.v("Unable to send message to: %s", client.id)
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
            Log.w("Unable to send broadcast message: %s", it)
        })
        return sent
    }
}
