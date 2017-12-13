package jp.sharelock.web

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
@groovy.transform.CompileStatic
class WebSocketServiceClient {
    private JettySession clientSession
    private Callable onMessageReceived
    private Callable onErrorReceived
    private WebSocketClient client

    // To be filled
    String hostname = "localhost"
    int port = 8888
    String path = ""
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
                onMessageReceived.call(JSON.decode(message).toMap())
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
    interface Callable {
        void call(Map message)
    }
    /**
     * Return JettySession (jetty Session) object
     * @return
     */
    JettySession getSession() {
        return clientSession
    }
    /**
     * Connects to a WS Server
     * @param URL : localhost:8888/something
     * @param onMessage
     * @param onError
     */
    void connect(Callable onMessage = null, Callable onError = null) {
        String serverURL = "ws://" + hostname + ":" + port + "/" + path
        client = new WebSocketClient()
        client.start()
        URI echoUri = new URI(serverURL)
        ClientUpgradeRequest request = new ClientUpgradeRequest()
        Future<JettySession> future = client.connect(new WSSocket(), echoUri, request)
        clientSession = future.get()
        onMessageReceived = onMessage
        onErrorReceived = onError
    }
    /**
     * Sends a message
     * @param message
     */
    void sendMessage(Map message) {
        sendMessage(JSON.encode(message))
    }
    /**
     * Sends a message
     * @param message
     */
    void sendMessage(String message) {
        clientSession.getRemote().sendString(message)
    }
    /**
     * Disconnects from server
     */
    void disconnect() {
        clientSession.disconnect()
        client.stop()
    }
}
