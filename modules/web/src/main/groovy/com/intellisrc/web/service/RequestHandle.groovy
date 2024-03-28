package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.session.SessionHandler

import static com.intellisrc.web.service.HttpHeader.ACCEPT
import static com.intellisrc.web.service.HttpHeader.UPGRADE
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500
import static org.eclipse.jetty.http.HttpStatus.getCode

/**
 * Handle the request from Jetty, including sessions
 * @since 2023/05/25.
 */
@CompileStatic
class RequestHandle extends SessionHandler {
    final protected WebService service
    final List<String> ignoreURIs = []

    RequestHandle(WebService service) {
        this.service = service
        setServer(service.server)
    }

    /**
     * Handle the request and convert Jetty objects to ICL objects
     *
     * @param target The target of the request - either a URI or a name.
     * @param jettyRequest The original unwrapped request object.
     * @param httpRequest The request either as the {@link org.eclipse.jetty.server.Request} object or a wrapper of that request.
     * @param httpResponse The response as the {@link org.eclipse.jetty.server.Response} object or a wrapper of that request.
     */
    @Override
    void doHandle(String target,  JettyRequest jettyRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Request request = new Request(jettyRequest)
        Response response = new Response(jettyRequest.response)
        boolean handled = false
        if(request.headers(UPGRADE) != "websocket" && request.headers(ACCEPT) != "text/event-stream") {
            if(ignoreURIs.empty || !(ignoreURIs.any { request.uri().matches(it) || target == it })) {
                try {
                    // Copy the template so we can keep it as instance (as WebException can only access Response object):
                    response.errorTemplate = service.errorTemplate
                    // Execute the filter
                    handled = service.doFilter(request, response)
                } catch (WebException we) {
                    boolean display = true
                    switch (true) {
                        case we.code >= INTERNAL_SERVER_ERROR_500:
                            Log.w("[%d] Request: [%s %s]. Exception in web response: %s", we.code, request.method, request.uri(), we.message)
                            break
                        case we.code >= BAD_REQUEST_400:
                            Log.w("[%d] Request: [%s %s]. Exception with the request: %s", we.code, request.method, request.uri(), we.message)
                            break
                        default:
                            display = false
                    }
                    if(display) {
                        // Ignore as it will just set the response to return error page
                        if (!we.text) {
                            we.text = getCode(we.code).message
                        } // Automatic
                        WebError webError = response.errorTemplate.call(we.code, we.text, response.type())
                        response.type(webError.contentType + (webError.charSet ? "; charset=" + webError.charSet : ""))
                        response.status(we.code)
                        response.writer.print(webError.content)
                        response.writer.flush()
                        response.writer.close()
                    }
                    handled = true
                }
            }
            // As response changes, update jetty response:
            // NOTE: jettyRequest.response is the same instance as httpResponse but with the Jetty class (httpResponse is an interface).
            //       So, updating jettyRequest.response here, will update httpResponse object as well
            response.update()
        }
        jettyRequest.setHandled(handled)
    }
}
