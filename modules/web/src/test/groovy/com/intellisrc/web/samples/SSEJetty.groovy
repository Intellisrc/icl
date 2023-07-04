package com.intellisrc.web.samples

import com.intellisrc.core.Millis
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.*

/**
 * @since 2023/07/04.
 */
class SSEJetty extends AbstractHandler {
    @Override
    void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        response.contentType = 'text/event-stream'
        response.characterEncoding = 'UTF-8'
        response.setHeader('Cache-Control', 'no-cache')
        response.setHeader('Connection', 'keep-alive')
        response.setHeader('Access-Control-Allow-Origin', '*')
        response.status = HttpServletResponse.SC_OK
        sleep(Millis.SECOND_5)
        (1..10).each {
            Map<String, String> msg = [
                id      : it.toString(),
                event   : "count",
                data    : "Hello from SSE Server: $it".toString()
            ]
            msg.each {
                response.writer.write(it.key + ":" + it.value + "\n")
            }
            response.writer.write("\n")
            response.writer.flush()
            sleep(Millis.SECOND_5)
        }
        response.writer.close()
    }
    static void main(String[] args) {
        def server = new Server(9999)
        // Configure resource handler for serving static files
        def resourceHandler = new ResourceHandler()
        resourceHandler.setDirectoriesListed(false)
        resourceHandler.setResourceBase('/tmp/sse/')

        // Create a context handler for the resource handler
        def contextHandler = new ContextHandler('/')
        contextHandler.setHandler(resourceHandler)

        // Create a context handler for the SSEHandler
        def sseContext = new ContextHandler('/sse/')
        sseContext.setHandler(new SSEJetty())

        // Set the handlers for the server
        server.handler = new HandlerList(contextHandler, sseContext, new DefaultHandler())

        server.start()
        server.join()
    }
}
