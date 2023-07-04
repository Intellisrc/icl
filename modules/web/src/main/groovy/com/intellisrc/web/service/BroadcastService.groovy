package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.util.concurrent.ConcurrentLinkedQueue

import static com.intellisrc.web.service.HttpHeader.X_FORWARDED_FOR

/**
 * This class provides the common code between ServerSendEvents and WebSockets
 * extending HttpServlet functionality
 * @since 2023/06/30.
 */
@CompileStatic
abstract class BroadcastService extends HttpServlet {
    /**
     * Return the user ID (required). If null or empty is returned, the session will be dropped.
     * @param params
     * @param remoteIP
     * @return
     */
    static interface SessionIdentifier {
        String call(HttpServletRequest request)
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
    static interface ClientListUpdated {
        void call(List<Client> list)
    }

    /**
     * Client to broadcast to
     */
    static class Client {
        protected final AsyncContext context
        final InetAddress ip
        final String id
        protected final int maxSize
        Client(HttpServletRequest request, String id, long timeout, int maxSize) {
            ip = request.remoteAddr.toInetAddress()
            this.id = id
            this.maxSize = maxSize
            context = request.startAsync()
            context.setTimeout(timeout)
        }
    }
    /**
     * Simple class to convert data to String
     */
    static class WebMessage {
        protected final Map data
        WebMessage(Map data) {
            this.data = data
        }
        WebMessage(Collection data) {
            this.data = [ _data_ : data ]
        }
        WebMessage(String data) {
            this.data = [ _data_ : data ]
        }
        String toString() {
            return JSON.encode(data.containsKey("_data_") ? data._data_ : data)
        }
        Map getData() {
            return data.containsKey("_data_") ? [ data : data._data_ ] : data
        }
    }

    /**
     * Get InetAddress from SocketAddress
     * @param socketAddress
     * @return
     */
    static InetAddress getAddressFromSocket(SocketAddress socketAddress) {
        return (socketAddress as InetSocketAddress).address
    }
    /**
     * Get the source IP address from session
     * @param sockSession
     * @return
     */
    static InetAddress getAddressFromSession(org.eclipse.jetty.websocket.api.Session sockSession) {
        return  sockSession.upgradeRequest.headers.containsKey(X_FORWARDED_FOR)
            ? sockSession.upgradeRequest.headers[X_FORWARDED_FOR].first().toInetAddress()
            : getAddressFromSocket(sockSession.remoteAddress)
    }

    //-------------------- INSTANCE ------------------------
    int maxSize = Config.get("web.event.max.size", 64) // KB
    protected final ConcurrentLinkedQueue<Client> clientList = new ConcurrentLinkedQueue<>()

    //TODO: document
    protected ClientListUpdated onClientListUpdated = {
        List<String> list ->
            Log.d("Number of clients connected: %d", list.size())
    }
    //TODO: document
    protected SessionIdentifier identifier = {
        HttpServletRequest request ->
            return request.requestedSessionId
    }

    protected long timeout = 0
    /**
     * Constructor
     * @param identifier
     * @param timeout
     */
    BroadcastService(SessionIdentifier identifier = null, long timeout = 0) {
        if(identifier) {
            this.identifier = identifier
        }
        this.timeout = timeout
    }
    /**
     * Send data to client
     * @param client
     * @param data
     * @return
     */
    abstract void sendTo(Client client, WebMessage message, SuccessCallback onSuccess, FailCallback onFail)

    /**
     * Disconnect or remove client
     * @param client
     * @return
     */
    boolean disconnectClient(Client client) {
        client.context?.complete()
        return clientList.remove(client)
    }
    /**
     * Perform GET action
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        clientList << new Client(req, identifier.call(req), timeout, maxSize)
        onClientListUpdated.call(clientList.toList())
    }
    /**
     * Send to all connected clients
     * @param data
     */
    void broadcast(Map data, SuccessCallback onSuccess = {}, FailCallback onFail = { Throwable e -> }) {
        if(! connected.empty) {
            connected.each {
                Client client ->
                    sendTo(client, new WebMessage(data), onSuccess, onFail)
            }
        }
    }
    /**
     * Search client by ID
     * @param id
     * @return
     */
    Optional<Client> get(String id) {
        return Optional.ofNullable(clientList.find { it.id == id })
    }
    /**
     * Get all clients
     * @return
     */
    List<Client> getConnected() {
        return clientList.toList()
    }
}
