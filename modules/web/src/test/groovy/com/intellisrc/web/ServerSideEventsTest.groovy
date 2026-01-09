package com.intellisrc.web

import com.intellisrc.net.LocalHost
import com.intellisrc.web.service.ServerSentEvent
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import spock.lang.Specification

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ServerSideEventsTest extends Specification {
    def messages = new CopyOnWriteArrayList<String>()
    def latch = new CountDownLatch(2)
    def connected = new CountDownLatch(1)

    class ServerSSE extends ServerSentEvent {
        String path = "/test"
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
                .url("http://localhost:${port}/test")
                .header("Accept", "text/event-stream")
                .build()

            EventSource.Factory factory = EventSources.createFactory(client)
            EventSource eventSource = factory.newEventSource(request, new ClientSSEListener())

        expect:
            connected.await(2, TimeUnit.SECONDS)

        when:
            sse.broadcast("Hello, World!")
            sse.broadcast("Another event")

        then:
            latch.await(2, TimeUnit.SECONDS)
            assert messages.toList() == ["Hello, World!", "Another event"]

        cleanup:
            eventSource.cancel()
            web.stop()
    }
}
