package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.net.LocalHost
import com.intellisrc.web.samples.ChatWebSocketClient
import com.intellisrc.web.samples.ChatWebSocketService
import com.intellisrc.web.samples.ChatWebSocketServiceIface
import com.intellisrc.web.samples.ChatWebSocketTestable
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.AsyncConditions

import java.util.concurrent.atomic.AtomicBoolean

import static com.intellisrc.web.samples.ChatWebSocketService.getRandomName

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
            connected.await(Millis.SECOND_5)
            cc.sendLoginMessage()

        then:
            received.await(Millis.SECOND_5)

        cleanup:
            cc.disconnect()
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

    def "Test path collision"() {

    }
}
