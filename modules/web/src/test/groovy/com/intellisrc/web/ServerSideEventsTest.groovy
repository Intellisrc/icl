package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.etc.JSON
import com.intellisrc.net.LocalHost
import com.intellisrc.web.service.ServerSentEvent
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.WebMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ServerSideEventsTest extends Specification {
    def messages = new CopyOnWriteArrayList<String>()
    def latch = new CountDownLatch(5)
    def connected = new CountDownLatch(1)
    static final ssePath = "/test"

    class ServerSSE extends ServerSentEvent {
        String path = ssePath
        OnClientConnect onClientConnect = { ->
            println("Client connected")
            connected.countDown()
        }
    }

    class ClientSSEListener extends EventSourceListener {
        @Override
        void onOpen(EventSource eventSource, Response response) {
            println("Connection Opened!")
        }

        @Override
        void onEvent(EventSource eventSource, String id, String type, String data) {
            println("New Event: " + data)
            messages << data
            latch.countDown()
        }

        @Override
        void onClosed(EventSource eventSource) {
            println("Connection Closed!")
        }

        @Override
        void onFailure(EventSource eventSource, Throwable t, Response response) {
            println("Error: " + t?.getMessage())
        }
    }

    def "should receive events from SSE Server"() {
        setup:
            def port = LocalHost.freePort
            def web = new WebService(port: port)
            def sse = new ServerSSE()
            web.addService(sse)
            web.start(true)

            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // Crucial: Don't timeout while reading the stream
                .build()

            Request request = new Request.Builder()
                .url("http://localhost:${port}/${ssePath}")
                .header("Accept", "text/event-stream")
                .build()

            EventSource.Factory factory = EventSources.createFactory(client)
            EventSource eventSource = factory.newEventSource(request, new ClientSSEListener())

        expect:
            connected.await(2, TimeUnit.SECONDS)

        when:
            sse.broadcast("Hello, World!")
            sse.broadcast("Another event")
            sse.broadcast([ hello : "world"])
            sse.broadcast(["one", "two"])
            sse.broadcast(new WebMessage("Something"))

        then:
            latch.await(2, TimeUnit.SECONDS)
            assert messages.toList() == [
                "Hello, World!",
                "Another event",
                JSON.encode([ hello : "world"]),
                JSON.encode(['one','two']),
                "Something"
            ]

        cleanup:
            eventSource.cancel()
            web.stop()
    }

    @Unroll
    def "SSE path and HTTP path should collide"() {
        setup:
            def port = LocalHost.freePort
            def web = new WebService(port: port)
            def sse = new ServerSSE()
            web.add(sse)
            web.add(new Service(
                path: webPath,
                action: {
                    Log.w("It shouldn't enter here")
                    assert false: "It shouldn't be called"
                }
            ))

        when:
            WebService.failOnCollision = throwExceptionFlag
            boolean exceptionThrown = false
            try {
                web.start(true)
            } catch(WebService.DuplicateException de) {
                exceptionThrown = true
                Log.i("Exception: %s", de)
            } catch (Exception e) {
                Log.e("Test failed: ", e)
            }

        then:
            assert exceptionThrown == shouldRaiseException: "It should throw exception on collision and flag"

        cleanup:
            web.isRunning() && web.stop()

        where:
            webPath        | throwExceptionFlag | shouldRaiseException
            ssePath        | true               | true
            ssePath        | false              | false
            ssePath + "/"  | true               | false // paths are not equal
            ssePath + "/"  | false              | false // paths are not equal
            ssePath + "/?" | true               | true  // ? = optional
            ssePath + "/?" | false              | false // ? = optional
    }
}
