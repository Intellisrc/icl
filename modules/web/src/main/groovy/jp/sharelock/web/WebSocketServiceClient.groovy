package jp.sharelock.web

import groovy.json.JsonSlurper
import static groovy.json.JsonOutput.toJson
import org.eclipse.jetty.websocket.api.Session as WebSocketSession
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
    private WebSocketSession clientSession
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
        void onConnect(WebSocketSession sockSession) throws Exception {
            clientSession = sockSession
        }

        @OnWebSocketMessage
        void onMessage(WebSocketSession sockSession, String message) {
            if(onMessageReceived != null) {
                JsonSlurper jsonSlurper = new JsonSlurper()
                onMessageReceived.call((Map) jsonSlurper.parseText(message))
            }
        }

        @OnWebSocketError
        void onWebSocketError(WebSocketSession sockSession, Throwable throwable) {
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
     * Return WebSocketSession (jetty Session) object
     * @return
     */
    WebSocketSession getSession() {
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
        Future<WebSocketSession> future = client.connect(new WSSocket(), echoUri, request)
        clientSession = future.get()
        onMessageReceived = onMessage
        onErrorReceived = onError
    }
    /**
     * Sends a message
     * @param message
     */
    void sendMessage(Map message) {
        sendMessage(toJson(message))
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
