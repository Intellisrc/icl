package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.web.WebService
import com.intellisrc.web.service.Request as RequestWrapper
import com.intellisrc.web.service.Response as ResponseWrapper
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.util.Callback

import static com.intellisrc.web.service.HttpHeader.ACCEPT
import static com.intellisrc.web.service.HttpHeader.UPGRADE
import static org.eclipse.jetty.http.HttpStatus.*
import static org.eclipse.jetty.server.Handler.Abstract

@CompileStatic
class RequestHandle extends Abstract {

    protected final WebService service
    final List<String> ignoreURIs = []

    RequestHandle(WebService service) {
        this.service = service
    }

    @Override
    boolean handle(
        Request httpRequest,
        Response httpResponse,
        Callback callback
    ) {
        RequestWrapper request = new RequestWrapper((HttpServletRequest) httpRequest)
        ResponseWrapper response = new ResponseWrapper((HttpServletResponse) httpResponse)

        boolean handled = false

        if (request.headers(UPGRADE) != "websocket" &&
            request.headers(ACCEPT) != "text/event-stream") {

            if (ignoreURIs.empty ||
                !(ignoreURIs.any { request.uri().matches(it) || request.uri() == it })) {

                try {
                    response.errorTemplate = service.errorTemplate
                    handled = service.doFilter(request, response)

                } catch (WebException we) {
                    if (!response.redirected) {
                        boolean display = true
                        switch (true) {
                            case we.code >= INTERNAL_SERVER_ERROR_500:
                                Log.w(
                                    "[%d] Request: [%s %s]. Exception in web response: %s",
                                    we.code, request.method, request.uri(), we.message
                                )
                                break
                            case we.code >= BAD_REQUEST_400:
                                Log.w(
                                    "[%d] Request: [%s %s]. Exception with the request: %s",
                                    we.code, request.method, request.uri(), we.message
                                )
                                break
                            default:
                                display = false
                        }

                        if (display) {
                            if (!we.text) {
                                we.text = getCode(we.code).message
                            }

                            WebError webError =
                                response.errorTemplate.call(
                                    we.code, we.text, response.type()
                                )

                            response.type(
                                webError.contentType +
                                    (webError.charSet
                                        ? "; charset=${webError.charSet}"
                                        : "")
                            )
                            response.status(we.code)
                            response.writer.print(webError.content)
                            response.writer.flush()
                            response.writer.close()
                        }
                    }
                    handled = true
                }
            }
        }

        if (handled) {
            //request.handled = true //FIXME: not sure if it is required now
            return true
        }

        return false
    }
}