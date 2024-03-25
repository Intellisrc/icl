package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.servlets.EventSource
import org.eclipse.jetty.servlets.EventSourceServlet

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This is the interface that you need to use
 * in order to provide SSE service
 * @since 2023/06/30.
 */
@CompileStatic
abstract class ServerSentEvent implements Serviciable {
    static int maxSize = Config.any.get("web.sse.max.size", 64) // KB

    @Override
    abstract String getPath()

    interface EventCallback<E,D> { void call(E event, D data) }

    private SSEServlet servlet = null
    SSEServlet getServlet() {
        if(! this.servlet) {
            this.servlet = new SSEServlet()
        }
        return this.servlet
    }

    final ConcurrentLinkedQueue<EventCallback<String, String>> onMessage = new ConcurrentLinkedQueue<>()

    class SSEServlet extends EventSourceServlet {
        @Override
        protected EventSource newEventSource(HttpServletRequest httpServletRequest) {
            boolean running = true
            return new EventSource() {
                @Override
                void onOpen(EventSource.Emitter emitter) throws IOException {
                    Log.d("Client connected")
                    EventCallback<String, String> evenCaller = (EventCallback<String, String>) {
                        String event, String message ->
                            try {
                                emitter.event(event, message)
                            } catch(Exception ignore) {
                                running = false
                            }
                    }
                    onMessage << evenCaller
                    Log.d("Clients: %d", onMessage.size())
                    while(running) {
                        sleep(Millis.MILLIS_100)
                    }
                    onMessage.remove(evenCaller)
                    Log.d("Client disconnected (remaining: %d)", onMessage.size())
                }

                @Override
                void onClose() {
                    running = false
                }
            }
        }
    }

    void broadcast(WebMessage wm, String event = "message") {
        String msg = wm.toString()
        if(msg.size() <= maxSize) {
            onMessage.each {
                it.call(event, msg)
            }
        } else {
            Log.w("Unable to send message. It is too large: %d > %d", msg.size(), maxSize)
        }
    }

    void broadcast(String text, String event = "message") {
        broadcast(new WebMessage(text))
    }

    void broadcast(Map map, String event = "message") {
        broadcast(new WebMessage(map))
    }

    void broadcast(Collection list, String event = "message") {
        broadcast(new WebMessage(list))
    }
}