package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlets.EventSource
import org.eclipse.jetty.servlets.EventSourceServlet

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This is a simple implementation of SSE Server using Jetty library only
 * @since 2023/07/04.
 */
class SSEJetty {
    static int port = 9998
    static String path = "sse"
    static int i = 0
    static String html = """
<html>
	<head>
		<title>SSE Example</title>
	</head>
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
			evtSource.onerror = (err, msg) => {
				const li = document.createElement("li");
				li.textContent = "Error: " + err;
				out.appendChild(li);
			};
			evtSource.addEventListener("count", (event) => {
				try {
					if(event.data) {
						const li = document.createElement("li");
						li.textContent = event.data;
						out.appendChild(li);
					}
				} catch {}
			});
		</script>
	</body>
</html>
"""
    interface Callback<T> { void call(T val) }
    static final ConcurrentLinkedQueue<Callback<Integer>> onCount = new ConcurrentLinkedQueue<>()

    static class SSEServlet extends EventSourceServlet {
        @Override
        protected EventSource newEventSource(HttpServletRequest httpServletRequest) {
            boolean running = true
            return new EventSource() {
                @Override
                void onOpen(EventSource.Emitter emitter) throws IOException {
                    Log.i("Client connected")
                    Callback<Integer> evenCaller = (Callback<Integer>) {
                        Integer count ->
                            try {
                                emitter.event("count", "Count: ${count}")
                            } catch(Exception ignore) {
                                running = false
                            }
                    }
                    onCount << evenCaller
                    Log.i("Clients: %d", onCount.size())
                    while(running) {
                        sleep(Millis.MILLIS_100)
                    }
                    onCount.remove(evenCaller)
                    Log.i("Client disconnected (remaining: %d)", onCount.size())
                }

                @Override
                void onClose() {
                    running = false
                }
            }
        }
    }

    static void main(String[] args) {
        File index = new File(File.tempDir, "index.html")
        index.text = html
        index.deleteOnExit()
        def server = new Server(port)

        // Configure resource handler for serving static files
        def resourceHandler = new ResourceHandler()
        resourceHandler.setDirectoriesListed(false)
        resourceHandler.setResourceBase(File.tempDir.absolutePath)

        def context = new ServletContextHandler(ServletContextHandler.SESSIONS)
        context.setContextPath("/")
        context.addServlet(new ServletHolder(new SSEServlet()), "/${path}")

        server.handler = new HandlerList(resourceHandler, context)

        //int count = 1000
        Thread.start {
            (0..1000).each {
                int num ->
                    i++
                    onCount.each {
                        Callback<Integer> callback ->
                            callback.call(i)
                    }
                    sleep(Millis.SECOND)
            }
        }

        println "Starting Jetty SSE Server in port $port"
        server.start()
        server.join()
    }
}
