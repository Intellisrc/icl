package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.web.WebService
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import static com.intellisrc.web.service.HttpHeader.ACCEPT
import static com.intellisrc.web.service.HttpHeader.UPGRADE
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500
import static org.eclipse.jetty.http.HttpStatus.getCode

/**
 * @since 2025/12/22.
 */
@CompileStatic
@TupleConstructor
class RequestFilter implements Filter {
    WebService service
    final List<String> ignoreURIs = []

    @Override
    void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        boolean handled = false

        HttpServletRequest httpReq = (HttpServletRequest) req
        HttpServletResponse httpRes = (HttpServletResponse) res

        Request request = new Request(httpReq)
        Response response = new Response(httpRes)

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

        if (!handled) {
            chain.doFilter(req, res)
        }
    }
}