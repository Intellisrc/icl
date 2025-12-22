package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import groovy.transform.CompileStatic
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncListener
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.ee10.servlet.DefaultServlet
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.util.resource.ResourceFactory

import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class SSEJetty {

    static int port = 9998
    static String path = "sse"
    static int counter = 0

    static String html = """
<html>
<head><title>SSE Example</title></head>
<body>
<ul id="out"></ul>
<script>
const evtSource = new EventSource("http://localhost:${port}/${path}");
const out = document.getElementById("out");

evtSource.onopen = () => {
  const li = document.createElement("li");
  li.textContent = "Connected";
  out.appendChild(li);
};

evtSource.addEventListener("count", e => {
  const li = document.createElement("li");
  li.textContent = e.data;
  out.appendChild(li);
});
</script>
</body>
</html>
"""

    interface Callback<T> { void call(T val) }

    static final ConcurrentLinkedQueue<Callback<Integer>> clients =
        new ConcurrentLinkedQueue<>()

    /* ------------------------------------------------------------ */
    /* SSE Servlet                                                  */
    /* ------------------------------------------------------------ */

    static class SSEServlet extends HttpServlet {

        @Override
        protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
        ) {
            resp.status = 200
            resp.contentType = "text/event-stream"
            resp.characterEncoding = "UTF-8"
            resp.setHeader("Cache-Control", "no-cache")
            resp.setHeader("Connection", "keep-alive")

            AsyncContext async = req.startAsync()
            async.timeout = 0

            PrintWriter out = resp.writer

            Callback<Integer> sender = { Integer value ->
                try {
                    out.print("event: count\n")
                    out.print("data: Count: ${value}\n\n")
                    out.flush()
                } catch (Throwable ignored) {
                    async.complete()
                }
            }

            clients.add(sender)
            Log.i("SSE client connected (%d)", clients.size())

            async.addListener([
                onComplete: { clients.remove(sender) },
                onTimeout : { clients.remove(sender) },
                onError   : { clients.remove(sender) }
            ] as AsyncListener)
        }
    }

    /* ------------------------------------------------------------ */
    /* Main                                                         */
    /* ------------------------------------------------------------ */

    static void main(String[] args) {

        File baseDir = File.tempDir
        File index = new File(baseDir, "index.html")
        index.text = html
        index.deleteOnExit()

        Server server = new Server(port)

        ServletContextHandler context =
            new ServletContextHandler(ServletContextHandler.SESSIONS)

        context.contextPath = "/"

        context.setBaseResource(
            ResourceFactory.of(server)
                .newResource(baseDir.toPath())
        )

        context.addServlet(
            new ServletHolder(new DefaultServlet()),
            "/"
        )

        server.handler = context

        Thread.start {
            while (true) {
                counter++
                clients.each { it.call(counter) }
                sleep(Millis.SECOND)
            }
        }

        println "Starting Jetty SSE Server on port $port"
        server.start()
        server.join()
    }
}
