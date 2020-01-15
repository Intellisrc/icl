package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.web.ServiciableWebSocket.WSMessage
import groovy.transform.CompileStatic
import org.eclipse.jetty.websocket.api.WriteCallback

import java.util.concurrent.ConcurrentLinkedQueue
import org.eclipse.jetty.websocket.api.Session as JettySession
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket

/**
 * This class is a wrapper for @WebSocket
 * All attempts to implement it programmatically failed. Tried:
 *  ServiciableWebSocket with @WebSocket : doesn't work (jetty complains) as interface, or as a extended class
 *  ServiciableWebSocket implements WebSocketListener : Fails to identify unique sessions
 *  ServiciableWebSocket extends WebSocketAdapter : same as above
 *
 * @since 17/04/21.
 */
@CompileStatic
@WebSocket
class WebSocketService {
    private ServiciableWebSocket listener
    /**
     * Interface used to send a message directly from client to server
     * Normally onMessage will return a WSMessage object which will
     * be sent back to clients. However in the case onMessage (client side)
     * contains Async operations, clients will need this interface
     * to send their messages in another thread.
     */
    interface MsgBroadCaster {
        void call(WSMessage message, SuccessCallback onSuccess, FailCallback onFail)
    }
    interface FailCallback {
        void call(Throwable e)
    }
    interface SuccessCallback {
        void call()
    }
    /**
     * Object which will be assigned to listener to be able to
     * broadcast outside the main thread
     */
    private final MsgBroadCaster broadCaster = {
        WSMessage wsMessage, SuccessCallback onSuccess, FailCallback onFail ->
            broadcast(wsMessage, onSuccess, onFail)
    } as MsgBroadCaster

    /**
     * Sets listener that will act as the websocket service provider.
     * Additionally, it sets a "MsgBroadCaster" interface which provides
     * a way to access this class "broadcast" method.
     * @param listener
     */
    WebSocketService(ServiciableWebSocket listener) {
        listener.broadCaster = broadCaster
        this.listener = listener
    }

    @OnWebSocketConnect
    void onConnect(JettySession sockSession) {
        String user = listener.getUserID(sockSession.upgradeRequest.parameterMap, sockSession.remoteAddress.address)
        Session session = sessionQueue.find {
            Session sess ->
                user == sess.userID
        }
        if(session) {
            if(listener.replaceOnDuplicate) {
                Log.v("Client replaced [%s] with userID: [%s]", sockSession.remoteAddress.address.hostAddress, user)
                session.websocketSession = sockSession
                session.address = sockSession.remoteAddress.address
                listener.onClientsChange(getConnected())
                broadcast(listener.onConnect(session))
            } else {
                Log.w("Client with userID [%s] already exits.", user)
            }
        } else {
            Log.i("Client connected [%s] with userID: [%s]", sockSession.remoteAddress.address.hostAddress, user)
            session = new Session(userID: user, websocketSession: sockSession, address: sockSession.remoteAddress.address)
            sessionQueue.add(session)
            listener.onClientsChange(getConnected())
            broadcast(listener.onConnect(session))
        }
    }

    @OnWebSocketClose
    void onClose(JettySession sockSession, int statusCode, String reason) {
        Log.i("Client disconnected [%s], reason: [%s]", sockSession.remoteAddress.address.hostAddress, reason)
        Session session = getSession(sockSession)
        sessionQueue.remove(session)
        listener.onClientsChange(getConnected())
        broadcast(listener.onDisconnect(session, statusCode, reason))
    }

    @OnWebSocketMessage
    void onMessage(JettySession sockSession, String message) {
        Log.v("[%s] sent message: [%s]", sockSession.remoteAddress.address.hostAddress, message)
        broadcast(listener.onMessage(getSession(sockSession), message))
    }

    @OnWebSocketError
    void onWebSocketError(JettySession sockSession, Throwable throwable) {
        listener.onError(getSession(sockSession), throwable.message)
        String ip = sockSession.remoteAddress.address.hostAddress
        if(throwable.message) {
            Log.e("[$ip] WebSocketError: ", throwable)
        } else {
            Log.w("[$ip] disconnected unexpectedly.")
        }
    }
    //------------- Implementation ---------------
    private Queue<Session> sessionQueue = new ConcurrentLinkedQueue()
    /**
     * Return list of peers
     * @return
     */
    private List<String> getConnected() {
        List<String> clients = []
        sessionQueue.each {
            Session sess ->
                if(sess.websocketSession.isOpen()) {
                    clients << sess.userID
                }
        }
        return clients
    }
    /**
     * Search for the Session using WebSocketSession
     */
    private Session getSession(JettySession sockSession) {
        return sessionQueue.find {
            Session sess ->
                return sess.websocketSession == sockSession
        }
    }
    /**
     * Search for the Session using UserID
     */
    private Session getSession(String userID) {
        return sessionQueue.find {
            Session sess ->
                sess.userID == userID
        }
    }

    /**
     * Send to users
     * if 'to' is set in WSMessage, it will send to specific recipients
     * @param message
     */
    private void broadcast(WSMessage wsMessage, SuccessCallback onSuccess = null, FailCallback onFail = null) {
        if(wsMessage != null) {
            if (wsMessage.to.isEmpty()) {
                wsMessage.to = getConnected()
            }
            wsMessage.to.each {
                String id ->
                    Session session = getSession(id)
                    if (session != null) {
                        try {
                            if (session.websocketSession.isOpen()) {
                                WriteCallback callback = new WriteCallback() {
                                    @Override
                                    void writeFailed(Throwable e) {
                                        if(onFail) {
                                            onFail.call(e)
                                        } else {
                                            Log.e("Failed to send message to client", e)
                                        }
                                    }
                                    @Override
                                    void writeSuccess() {
                                        if(onSuccess) {
                                            onSuccess.call()
                                        } else {
                                            Log.v("Message was sent successfully")
                                        }
                                    }
                                }
                                session.websocketSession.remote.sendString(JSON.encode(wsMessage.jsonObj), callback)
                            }
                        } catch (Exception e) {
                            listener.onError(session, e.message)
                        }
                    }
            }
        }
    }
}
