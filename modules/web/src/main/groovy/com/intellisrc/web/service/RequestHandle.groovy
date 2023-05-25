package com.intellisrc.web.service

import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.session.SessionHandler

/**
 * Handle the request from Jetty, including sessions
 * @since 2023/05/25.
 */
@CompileStatic
class RequestHandle extends SessionHandler {
    final protected WebService service

    RequestHandle(WebService service) {
        this.service = service
        setServer(service.server)
    }

    @Override
    void doHandle(String target,  JettyRequest jettyRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Request request = Request.import(jettyRequest)
        Response response = Response.import(jettyRequest.response)
        HttpMethod method = HttpMethod.fromString(request.method.trim().toUpperCase())
        if(method == null) {
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED_405)
            return
        }
        service.doFilter(request, response)
        request.setHandled(response.status > 0)
    }
}
