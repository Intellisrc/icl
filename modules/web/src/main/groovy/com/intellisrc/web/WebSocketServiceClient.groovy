package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic
import org.eclipse.jetty.websocket.api.Session as JettySession
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

import java.util.concurrent.Future

/**
 * Wrapper for org.eclipse.jetty.websocket.client.WebSocketClient that serves
 * to connect to a WebSocket Server
 * @since 17/04/24.
 */
@CompileStatic
class WebSocketServiceClient {
    protected Callable onMessageReceived
    protected Callable onErrorReceived
    protected JettySession clientSession
    protected WebSocketClient client
    protected URI url
    /**
     * WebSocket which will handle the connection
     */
    @WebSocket
    class WSSocket {
        @OnWebSocketConnect
        void onConnect(JettySession sockSession) throws Exception {
            clientSession = sockSession
        }

        @OnWebSocketMessage
        void onMessage(JettySession sockSession, String message) {
            if(onMessageReceived != null) {
                onMessageReceived.call(JSON.decode(message) as Map)
            }
        }

        @OnWebSocketError
        void onWebSocketError(JettySession sockSession, Throwable throwable) {
            if(onErrorReceived != null) {
                onErrorReceived.call(
                        error: throwable.message,
                        cause: throwable.cause,
                        localized : throwable.localizedMessage,
                        trace : throwable.getStackTrace().join("\n")
                )
            }
        }
    }
    /**
     * Interface used as callback for onMessage and onError
     */
    static interface Callable {
        void call(Map message)
    }

    WebSocketServiceClient(URI uri) {
        this.url = uri
    }
    WebSocketServiceClient(URL url) {
        this.url = url.toURI()
    }
    WebSocketServiceClient(Map<String, Object> map) {
        if(!map.protocol)   { map.protocol = "ws" }
        if(!map.hostname)   { map.hostname = "localhost" }
        if(!map.port)       { map.port = 8000 }
        if(!map.path)       { map.path = "/" }
        this.url = new URI(map.protocol.toString() + "//" + map.hostname.toString() + ":" + map.port.toString() + map.path.toString() )
    }

    /**
     * Return JettySession (jetty Session) object
     * @return
     */
    JettySession getSession() {
        return clientSession
    }

    /**
     * Return Jetty websocket client
     * @return
     */
    WebSocketClient getClient() {
        return client
    }
    /**
     * Connects to a WS Server
     * @param URL : localhost:8888/something
     * @param onMessage
     * @param onError
     */
    void connect(Callable onMessage = null, Callable onError = null) {
        client = new WebSocketClient()
        client.start()
        ClientUpgradeRequest request = new ClientUpgradeRequest()
        Future<JettySession> future = client.connect(new WSSocket(), url, request)
        clientSession = future.get()
        onMessageReceived = onMessage
        onErrorReceived = onError
    }
    /**
     * Returns true if client is connected
     * @return
     */
    boolean isConnected() {
        return clientSession && clientSession.open
    }
    /**
     * Sends a message
     * @param message
     */
    void sendMessage(Map message) {
        if(message &&! message.isEmpty()) {
            sendMessage(JSON.encode(message))
        } else {
            Log.d("Trying to send an empty message")
        }
    }
    /**
     * Sends a message
     * @param message
     */
    void sendMessage(String message) {
        if(message) {
            clientSession.getRemote().sendString(message)
        } else {
            Log.d("Trying to send an empty message")
        }
    }
    /**
     * Disconnects from server
     */
    void disconnect() {
        clientSession.disconnect()
        client.stop()
    }
}
