package com.intellisrc.web.service

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This is the interface that you need to use
 * in order to provide SSE service
 * @since 2023/06/30.
 */
@CompileStatic
abstract class ServerSentEvent implements Serviciable {
    static int maxSize          = Config.any.get("web.sse.max.size", 64) // KB
    static String contentType   = Config.any.get("web.sse.content.type", "text/event-stream")
    static String encoding      = Config.any.get("web.sse.encoding", "UTF-8")

    abstract String getPath()
    interface OnClientConnect { void call() }
    OnClientConnect onClientConnect = null
    private final ConcurrentLinkedQueue<AsyncContext> clients = new ConcurrentLinkedQueue<>()

    HttpServlet servlet = new HttpServlet() {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            resp.setStatus(200)
            resp.setContentType(contentType)
            resp.setCharacterEncoding(encoding)
            resp.setHeader("Cache-Control", "no-cache")
            resp.setHeader("Connection", "keep-alive")

            AsyncContext async = req.startAsync()
            async.setTimeout(0)

            clients.add(async)

            if(onClientConnect) {
                onClientConnect.call()
            }

            async.addListener([
                onComplete: { clients.remove(async) },
                onTimeout : { clients.remove(async) },
                onError   : { clients.remove(async) }
            ] as AsyncListener)
        }
    }

    void broadcast(WebMessage wm, String event = "message") {
        String msg = wm.toString()
        if(msg.size() <= maxSize) {
            clients.each { AsyncContext async ->
                try {
                    PrintWriter out = async.response.writer
                    out.write("event: $event\n")
                    out.write("data: $msg\n\n")
                    out.flush()
                } catch (Exception e) {
                    clients.remove(async)
                    async.complete()
                    Log.d("Exception while broadcasting: %s", e)
                }
            }
        } else {
            Log.w("Unable to send message. It is too large: %d > %d", msg.size(), maxSize)
        }
    }

    void broadcast(String text, String event = "message") {
        broadcast(new WebMessage(text), event)
    }

    void broadcast(Map map, String event = "message") {
        broadcast(new WebMessage(map), event)
    }

    void broadcast(Collection list, String event = "message") {
        broadcast(new WebMessage(list), event)
    }
}
