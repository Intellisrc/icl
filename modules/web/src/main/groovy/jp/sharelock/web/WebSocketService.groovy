package jp.sharelock.web

import jp.sharelock.etc.Log
import jp.sharelock.web.ServiciableWebSocket.WSMessage

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
@groovy.transform.CompileStatic
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
        void call(WSMessage message)
    }
    /**
     * Object which will be assigned to listener to be able to
     * broadcast outside the main thread
     */
    private final MsgBroadCaster broadCaster = {
        WSMessage wsMessage ->
            broadcast(wsMessage)
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
    void onConnect(JettySession sockSession) throws Exception {
        String user = listener.getUserID(sockSession.upgradeRequest.parameterMap, sockSession.getRemoteAddress().address)
        Session session = new Session(userID: user, websocketSession: sockSession, address: sockSession.getRemoteAddress().address)
        sessionQueue.add(session)
        listener.onClientsChange(getConnected())
        broadcast(listener.onConnect(session))
    }

    @OnWebSocketClose
    void onClose(JettySession sockSession, int statusCode, String reason) {
        Session session = getSession(sockSession)
        sessionQueue.remove(session)
        listener.onClientsChange(getConnected())
        broadcast(listener.onDisconnect(session, statusCode, reason))
    }

    @OnWebSocketMessage
    void onMessage(JettySession sockSession, String message) {
        broadcast(listener.onMessage(getSession(sockSession), message))
    }

    @OnWebSocketError
    void onWebSocketError(JettySession sockSession, Throwable throwable) {
        listener.onError(getSession(sockSession), throwable.message)
        Log.e( throwable.message)
    }
    //------------- Implementation ---------------
    private Queue<Session> sessionQueue = new ConcurrentLinkedQueue()
    /**
     * Return list of peers
     * @return
     */
    private List<String> getConnected() {
        List<String> clients = new ArrayList<>()
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
                return sess.websocketSession.isOpen() && sess.websocketSession == sockSession
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
    private void broadcast(WSMessage wsMessage) {
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
                                session.websocketSession.getRemote().sendString(JSON.encode(wsMessage.jsonObj))
                            }
                        } catch (Exception e) {
                            listener.onError(session, e.message)
                        }
                    }
            }
        }
    }
}
