package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.net.LocalHost
import com.intellisrc.web.samples.ChatWebSocketClient
import com.intellisrc.web.samples.ChatWebSocketService
import com.intellisrc.web.samples.ChatWebSocketServiceIface
import com.intellisrc.web.samples.ChatWebSocketTestable
import com.intellisrc.web.service.Service
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.AsyncConditions

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*
import static com.intellisrc.web.samples.ChatWebSocketService.getRandomName
import static org.eclipse.jetty.http.HttpStatus.OK_200

/**
 * @since 2026/01/05.
 */
class WebSocketTest extends Specification {
    @Unroll
    def "Websocket Test"() {
        setup:
            def connected = new AsyncConditions(1)
            def received  = new AsyncConditions(1)

            def keepalive = false
            def chatPort = LocalHost.freePort

            def web = new WebService(
                port: chatPort,
                resources: System.getProperty("user.dir") + "/res/public/",
                cacheTime: 60
            )

            web.addService(chatService)
            web.start(!keepalive)

        when:
            String userId = randomName
            ChatWebSocketClient cc = new ChatWebSocketClient(chatPort, chatService.path, userId)
            AtomicBoolean connectedSeen = new AtomicBoolean(false)

            cc.handler = { Map msg ->
                Log.i("Message replied: %s", msg.message)
                assert msg.type == "txt"
                if (msg.message == "Connected") {
                    connectedSeen.set(true)
                    connected.evaluate {
                        assert (msg.list as List).size() == 1
                        assert (msg.list as List).contains(userId)
                    }
                }
                if (msg.message == "Received") {
                    assert connectedSeen.get() : "Message received without connecting first"
                    received.evaluate {
                        assert (msg.list as List).size() == 1
                    }
                }
            }

        then:
            assert web.isRunning() : "Web is not running"
            assert cc.connect() : "Not connected"

        when:
            connected.await(SECOND_15)
            cc.sendLoginMessage()

        then:
            received.await(SECOND_15)

        cleanup:
            cc.disconnect()
            sleep(MILLIS_200) // Let Gitlab CI to get the message.
            def test = chatService as ChatWebSocketTestable
            assert   test.disconnectWasCalled           : "Disconnection was not processed"
            assert   test.clientList.empty              : "Users were not removed"
            assert ! test.clientList.contains(userId)   : "User should not be in list"
            web.stop()

        where:
            chatService                     | serviceName
            new ChatWebSocketService()      | "Chat WebSocket extends"
            new ChatWebSocketServiceIface() | "Chat WebSocket implements"
    }

    def "WebSockets paths should not collide with HTTP paths"() {
        AtomicInteger getPathCount = new AtomicInteger(0)
        setup:
            int port = LocalHost.freePort
            def web = new WebService(
                port: port
            )
            Log.i("Running in port: %d", port)
            def chatService = new ChatWebSocketService()
            web.add(new Service(
                path: "/chat",
                action: {
                    Log.i("HTTP GET ${chatService.path} is working")
                    getPathCount.incrementAndGet()
                    return "ok"
                }
            ))
            web.add(chatService)
            web.start(true)
        when:
            URL url = ("http://localhost:" + port + chatService.path).toURL()
            def conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
        then:
            assert conn.responseCode == OK_200 : "Incorrect response code"
            assert getPathCount.get() == 1 : "GET method was not accessible"
        when:
            String userId = randomName
            ChatWebSocketClient cc = new ChatWebSocketClient(port, chatService.path, userId)
            def connected = new CountDownLatch(1)

            cc.handler = { Map msg ->
                Log.i("Message replied: %s", msg.message)
                assert msg.type == "txt"
                if (msg.message == "Connected") {
                    connected.countDown()
                }
            }
        then:
            assert web.isRunning() : "Web is not running"
            assert cc.connect() : "Not connected"
            connected.await(2, TimeUnit.SECONDS)

        cleanup:
            cc.disconnect()
            web.stop()
    }
}
