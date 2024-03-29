
import Config
import Log
import Millis
import ServiciableWebSocket
import CompileStatic
import Session as WebSocketSession
import org.eclipse.jetty.websocket.api.annotations.*

import java.time.Duration

import static BroadcastService.*

/**
 * This class is a wrapper for @WebSocket
 *
 * @since 17/04/21.
 */
@CompileStatic
@WebSocket
@Deprecated
class WebSocketService extends WebServiceBase {
    int maxSize = Config.get("websocket.max.size", 64)
    protected ServiciableWebSocket listener
    /**
     * Object which will be assigned to listener to be able to
     * broadcast outside the main thread
     */
    final MsgBroadCaster broadCaster = {
        WebMessage wsMessage, SuccessCallback onSuccess, FailCallback onFail ->
            broadcast(wsMessage, onSuccess, onFail)
    } as MsgBroadCaster

    /**
     * Sets listener that will act as the websocket service provider.
     * Additionally, it sets a "MsgBroadCaster" interface which provides
     * a way to access this class "broadcast" method.
     * @param listener
     */
    WebSocketService(ServiciableWebSocket listener) {
        //FIXME listener.broadCaster = broadCaster
        this.listener = listener
    }

    @OnWebSocketConnect
    void onConnect(Session sockSession) {
        if(sockSession) {
            // Set limits
            sockSession.idleTimeout = Duration.ofMillis(timeout * Millis.SECOND)
            sockSession.policy.maxBinaryMessageSize =
                    sockSession.policy.maxTextMessageSize =
                            sockSession.policy.inputBufferSize = maxSize * 1024

            InetAddress address = getAddressFromSession(sockSession)
            if(address) {
                String user = "" //FIXME listener.getUserID(sockSession.upgradeRequest.parameterMap, address)
                if (!user) {
                    Log.w("User connected had no userID")
                    sockSession.close()
                } else {
                    //FIXME Session session = sessionCtrl.find(sockSession)
                    /* FIXME:
                    if (session) {
                        if (listener.replaceOnDuplicate) {
                            Log.v("Client replaced [%s] with userID: [%s]", address.hostAddress, user)
                            session.websocketSession = sockSession
                            session.address = address
                            listener.onClientsChange(getConnected())
                            broadcast(listener.onConnect(session))
                        } else {
                            Log.w("Client with userID [%s] already exits.", user)
                        }
                    } else {
                        Log.i("Client connected [%s] with userID: [%s]", address.hostAddress, user)
                        session = sessionCtrl.create(sockSession)
                        listener.onClientsChange(getConnected())
                        broadcast(listener.onConnect(session))
                    }*/
                }
            } else {
                Log.w("Unable to recognize source address")
            }
        }
    }

    @OnWebSocketClose
    void onClose(Session sockSession, int statusCode, String reason) {
        if(sockSession) {
            InetAddress address = getAddressFromSession(sockSession)
            Log.i("Client disconnected [%s], reason: [%s]", address?.hostAddress ?: "unknown", reason)
            /*FIXME Session session = sessionCtrl.find(sockSession)
            session?.invalidate()
            broadcast(listener.onDisconnect(session, statusCode, reason))*/
        }
        //FIXME listener.onClientsChange(getConnected())
    }

    @OnWebSocketMessage
    void onMessage(Session sockSession, String message) {
        if(sockSession) {
            InetAddress address = getAddressFromSession(sockSession)
            Log.v("[%s] sent message: [%s]", address?.hostAddress ?: "unknown", message)
            //FIXME broadcast(listener.onMessage(getSession(sockSession), message))
        }
    }

    @OnWebSocketError
    void onWebSocketError(Session sockSession, Throwable throwable) {
        if(sockSession) {
            //FIXME listener.onError(sessionCtrl.find(sockSession), throwable.message)
            InetAddress address = getAddressFromSession(sockSession)
            String ip = address?.hostAddress ?: "unknown"
            if (throwable.message) {
                Log.e("[$ip] WebSocketError: ", throwable)
            } else {
                Log.w("[$ip] disconnected unexpectedly.")
            }
        }
    }
    //------------- Implementation ---------------
    /**
     * Return list of peers
     * @return
     */
    private List<String> getConnected() {
        List<String> clients = []
        /* FIXME
        sessionQueue.each {
            Session sess ->
                if(sess.websocketSession.isOpen()) {
                    clients << sess.userID
                }
        }*/
        return clients
    }

    /**
     * Send to users
     * if 'to' is set in WSMessage, it will send to specific recipients
     * @param message
     */
    protected void broadcast(WebMessage wsMessage, SuccessCallback onSuccess = null, FailCallback onFail = null) {
        if(wsMessage != null) {
            if (wsMessage.to.isEmpty()) {
                wsMessage.to = getConnected()
            }
            wsMessage.to.each {
                String id ->
                    /* FIXME
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
                    } */
            }
        }
    }
}
