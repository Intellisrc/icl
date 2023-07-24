package com.intellisrc.web.service

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.eclipse.jetty.websocket.api.Session as JettySession

import java.util.concurrent.ConcurrentLinkedQueue

import static com.intellisrc.web.service.HttpHeader.X_FORWARDED_FOR

/**
 * This class provides the common code between ServerSendEvents and WebSockets
 * extending HttpServlet functionality
 * @since 2023/06/30.
 */
@CompileStatic
trait BroadcastService {
    /**
     * Return the user ID (required). If null or empty is returned, the session will be dropped.
     * @param params
     * @param remoteIP
     * @return
     */
    static interface SessionIdentifier {
        String call(Request request)
    }
    /**
     * Interface used to send a message directly from client to server
     * Normally onMessage will return a WSMessage object which will
     * be sent back to clients. However in the case onMessage (client side)
     * contains Async operations, clients will need this interface
     * to send their messages in another thread.
     */
    static interface MsgBroadCaster {
        void call(WebMessage message, SuccessCallback onSuccess, FailCallback onFail)
    }
    static interface FailCallback {
        void call(Throwable e)
    }
    static interface SuccessCallback {
        void call()
    }
    static interface ClientConnected {
        void call(EventClient client)
    }
    static interface ClientDisconnected {
        void call(EventClient client)
    }
    static interface MessageReceived {
        void call(EventClient client, WebMessage message)
    }
    static interface ClientListUpdated {
        void call(List<EventClient> list)
    }

    /**
     * Get InetAddress from SocketAddress
     * @param socketAddress
     * @return
     */
    InetAddress getAddressFromSocket(SocketAddress socketAddress) {
        return (socketAddress as InetSocketAddress).address
    }
    /**
     * Get the source IP address from session
     * @param sockSession
     * @return
     */
    InetAddress getAddressFromSession(JettySession sockSession) {
        return  sockSession.upgradeRequest.headers.containsKey(X_FORWARDED_FOR)
            ? sockSession.upgradeRequest.headers[X_FORWARDED_FOR].first().toInetAddress()
            : getAddressFromSocket(sockSession.remoteAddress)
    }

    //-------------------- INSTANCE ------------------------
    final ConcurrentLinkedQueue<EventClient> clientList = new ConcurrentLinkedQueue<>()

    //TODO: document
    ClientListUpdated onClientListUpdated = {
        List<EventClient> list ->
            Log.v("Number of clients connected: %d", list.size())
    }

    ClientConnected onClientConnect = {
        EventClient client ->
            Log.v("Client connected: %s", client.id)
    }

    ClientDisconnected onClientDisconnect = {
        EventClient client ->
            Log.v("Client disconnected: %s", client.id)
    }

    MessageReceived onMessageReceived = {
        EventClient client, WebMessage msg ->
            Log.v("Client [%s] sent message: [%s]", client.id, msg.toString())
    }

    //TODO: document
    SessionIdentifier identifier = {
        Request request ->
            return request.requestedSessionId ?: request.remoteUser ?: request.remoteAddr
    }

    long timeout = 0
    /**
     * Send data to client
     * @param client
     * @param data
     * @return
     */
    abstract void sendTo(EventClient client, WebMessage message, SuccessCallback onSuccess, FailCallback onFail)

    /**
     * Disconnect or remove client
     * @param client
     * @return
     */
    boolean disconnectClient(EventClient client) {
        onClientDisconnect.call(client)
        client.context?.complete()
        return clientList.remove(client)
    }
    /**
     * Send to all connected clients
     * @param data
     */
    void broadcast(WebMessage wm, SuccessCallback onSuccess = {}, FailCallback onFail = { Throwable e -> }) {
        if(! connected.empty) {
            connected.each {
                EventClient client ->
                    sendTo(client, wm, onSuccess, onFail)
            }
        }
    }
    /**
     * Search client by ID
     * @param id
     * @return
     */
    Optional<EventClient> get(String id) {
        return Optional.ofNullable(clientList.find { it.id == id })
    }
    /**
     * Get all clients
     * @return
     */
    List<EventClient> getConnected() {
        return clientList.toList()
    }
}
