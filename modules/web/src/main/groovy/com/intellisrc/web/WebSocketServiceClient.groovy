package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic
import jakarta.websocket.*
import jakarta.websocket.ClientEndpointConfig.Configurator
import org.eclipse.jetty.ee10.websocket.jakarta.client.JakartaWebSocketClientContainer

/**
 * WebSocket client (Jetty 12 / Jakarta WebSocket)
 * @since 17/04/24.
 */
@CompileStatic
class WebSocketServiceClient {

    boolean async   = true    // Turn off to warranty delivery
    String protocol = "ws"
    String hostname = "localhost"
    String path     = "/"
    int port        = 8000

    protected Callable onMessageReceived
    protected Callable onErrorReceived

    protected Session clientSession
    protected JakartaWebSocketClientContainer container
    protected URI uri

    /**
     * WebSocket endpoint
     */
    class WSSocket extends Endpoint {

        @Override
        void onOpen(Session session, EndpointConfig config) {
            clientSession = session

            session.addMessageHandler(String, (MessageHandler.Whole<String>) {
                String message ->
                    if (onMessageReceived) {
                        onMessageReceived.call(JSON.decode(message) as Map)
                    }
            })
        }

        @Override
        void onError(Session session, Throwable throwable) {
            if (onErrorReceived) {
                onErrorReceived.call([
                    error     : throwable.message,
                    cause     : throwable.cause,
                    localized : throwable.localizedMessage,
                    trace     : throwable.stackTrace.join("\n")
                ])
            }
        }

        @Override
        void onClose(Session session, CloseReason reason) {
            Log.v("WebSocket closed: %s", reason)
        }
    }

    /**
     * Interface used as callback
     */
    static interface Callable {
        void call(Map message)
    }

    WebSocketServiceClient(URI uri) {
        this.uri = uri
    }

    WebSocketServiceClient(URL url) {
        this.uri = url.toURI()
    }

    WebSocketServiceClient() {}

    URI getURL() {
        if(! this.uri) {
            this.uri = new URI(
                "${protocol}://${hostname}:${port}${path}"
            )
        }
        return this.uri
    }

    Session getSession() {
        return clientSession
    }

    /**
     * Connect to WebSocket server
     */
    void connect(Callable onMessage = null, Callable onError = null) {
        this.onMessageReceived = onMessage
        this.onErrorReceived = onError

        container = new JakartaWebSocketClientContainer()
        container.start()

        ClientEndpointConfig config =
            ClientEndpointConfig.Builder.create()
                .configurator(new Configurator() {})
                .build()

        container.connectToServer(
            new WSSocket(),
            config,
            getURL()
        )
    }

    boolean isConnected() {
        return clientSession?.open
    }

    void sendMessage(Map message) {
        if (message && !message.isEmpty()) {
            sendMessage(JSON.encode(message))
        } else {
            Log.v("Trying to send an empty message")
        }
    }

    void sendMessage(String message) {
        if (message && clientSession?.open) {
            try {
                if (async) {
                    clientSession.asyncRemote.sendText(message)
                } else {
                    clientSession.basicRemote.sendText(message)
                }
            } catch(Exception e) {
                Log.w("Unable to send message: %s", e)
            }
        } else {
            Log.v("WebSocket not connected or empty message")
        }
    }

    void disconnect() {
        clientSession?.close()
        container?.stop()
    }
}