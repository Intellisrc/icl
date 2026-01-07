package com.intellisrc.web.service

import com.intellisrc.core.Log
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse

/**
 * Jetty 12 compatible Response wrapper with servlet delegation.
 */
class Response {

    /** Delegate ONLY the servlet API */
    @Delegate
    final HttpServletResponse servlet

    WebError.WebErrorTemplate errorTemplate = null
    Compression compression = Compression.NONE
    boolean redirected = false

    Response(ServletResponse response) {
        this.servlet = (HttpServletResponse) response
    }

    /* ------------------------------------------------------------ */
    /* Spark-style compatibility methods                            */
    /* ------------------------------------------------------------ */

    void status(int code) {
        servlet.setStatus(code)
    }

    void redirect(String path) {
        redirected = true
        servlet.sendRedirect(path)
    }

    void sendError(int code) {
        throw new WebException(code)
    }
    void sendError(int code, String msg) {
        throw new WebException(code, msg)
    }

    void type(String type) {
        servlet.setContentType(type)
    }

    String type() {
        return servlet.getContentType()
    }

    int getLength() {
        String len = servlet.getHeader("Content-Length")
        return len ? len as int : 0
    }

    void header(String key, String value) {
        if (key == "Date") return

        String existing = servlet.getHeader(key)
        if (existing != null && existing != value) {
            Log.v(
                "HTTP Header: '%s' already existed. Replaced: %s -> %s",
                key, existing, value
            )
        }
        servlet.setHeader(key, value)
    }

    String header(String key) {
        return servlet.getHeader(key)
    }

    Map<String, String> getHeaders() {
        Map<String, String> insensitiveMap =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER)

        servlet.getHeaderNames().each { name ->
            String value = servlet.getHeader(name)
            if (value) {
                insensitiveMap[name] = value
            }
        }
        return Collections.unmodifiableMap(insensitiveMap)
    }
}