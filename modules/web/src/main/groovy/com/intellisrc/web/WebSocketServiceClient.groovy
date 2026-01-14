package com.intellisrc.web

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.etc.JSON
import groovy.transform.CompileStatic
import jakarta.websocket.*
import jakarta.websocket.ClientEndpointConfig.Configurator
import org.eclipse.jetty.ee10.websocket.jakarta.client.JakartaWebSocketClientContainer

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * WebSocket client (Jetty 12 / Jakarta WebSocket)
 * @since 17/04/24.
 */
@CompileStatic
class WebSocketServiceClient {
    int maxSizeClient = Config.any.get("web.ws.client.max.size", 64) // KB
    int connectTimeout = Config.any.get("web.ws.client.timeout", Millis.SECOND_5) // ms
    int idleTimeout = Config.any.get("web.ws.client.idle.timeout", Millis.MINUTE) // ms
    int sendTimeout = Config.any.get("web.ws.client.send.timeout", Millis.SECOND_10) // ms

    boolean async   = true    // Turn off to warranty delivery
    String protocol = "ws"
    String hostname = "localhost"
    String path     = "/"
    int port        = 8000

    protected Callable onMessageReceived
    protected Callable onErrorReceived
    CountDownLatch opened = new CountDownLatch(1)

    protected Session clientSession
    protected JakartaWebSocketClientContainer container
    protected URI uri

    /**
     * WebSocket endpoint
     */
    class WSSocket extends Endpoint {

        @Override
        void onOpen(Session session, EndpointConfig config) {
            opened.countDown()
            clientSession = session

            clientSession.asyncRemote.sendTimeout = sendTimeout
            session.setMaxIdleTimeout(idleTimeout)
            session.maxTextMessageBufferSize = maxSizeClient * 1024
            session.maxBinaryMessageBufferSize = maxSizeClient * 1024

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
    void connect(Callable onMessage = null, Callable onError = null) throws TimeoutException {
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
        if (!opened.await(connectTimeout, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("WebSocket connect timeout")
        }
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
        try {
            if (clientSession?.open) {
                clientSession.close(
                    new CloseReason(
                        CloseReason.CloseCodes.NORMAL_CLOSURE,
                        "Client disconnect"
                    )
                )
            }
        } finally {
            container?.stop()
        }
    }
}