package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.util.IO

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
class Request {

    static final String X_FORWARDED_FOR = "X-Forwarded-For"
    static final String SESSION_ID = "JSESSIONID"

    /** Servlet API request */
    @Delegate
    final HttpServletRequest servlet

    final ConcurrentHashMap<String, String> pathParameters = new ConcurrentHashMap<>()
    protected String splat = ""

    Request(ServletRequest request) {
        this.servlet = (HttpServletRequest) request
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Not an HTTP request")
        }
    }

    // ---------------- IP / HOST ----------------

    String ip() { getIp() }

    String getIp() {
        String forwarded = getHeader(X_FORWARDED_FOR)
        return forwarded ?: getRemoteAddr()
    }

    String uri() {
        return getRequestURI()
    }

    String host() { getHost() }

    String getHost() {
        String local = getLocalName()
        if (!(local =~ /[a-zA-Z]+/)) {
            try {
                local.toInetAddress()
                local = getHostHeader().tokenize(":").first()
            } catch (Exception ignore) {}
        }
        return local
    }

    String getAddress() {
        return getLocalAddr()
    }

    String getHostHeader() {
        return getHeader("Host") ?: "localhost"
    }

    int getPort() {
        return getLocalPort()
    }

    String scheme() {
        return getScheme()
    }

    // ---------------- HEADERS / ATTRIBUTES ----------------

    String headers(String key) {
        return getHeader(key)
    }

    String attribute(String key) {
        return (String) getAttribute(key)
    }

    void attribute(String key, Object value) {
        setAttribute(key, value)
    }

    List<Compression> getAcceptedEncodings() {
        String enc = getHeader(HttpHeader.ACCEPT_ENCODING.asString())
        return enc
            ? enc.tokenize(",")
            .collect { Compression.fromString(it.trim()) }
            .sort { it.ordinal() }
            : []
    }

    // ---------------- PATH PARAMS ----------------

    void setPathParameters(Map<String, String> params) {
        params.each { k, v ->
            if (k == "splat") splat = v
            else pathParameters.put(k, v)
        }
    }

    String params(String key) {
        return getPathParam(key)
    }

    String getPathParam(String key) {
        return pathParameters.getOrDefault(key, "")
    }

    Set<String> params() {
        return pathParameters.keySet()
    }

    Map<String, String> getPathParams() {
        return Collections.unmodifiableMap(pathParameters)
    }

    boolean hasPathParams() {
        return !pathParameters.isEmpty()
    }

    List<String> splat() {
        return splat ? splat.tokenize("/") : []
    }

    // ---------------- QUERY PARAMS ----------------

    String queryParams(String key) {
        return getQueryParam(key)
    }

    String getQueryParam(String key) {
        return getParameter(key)
    }

    List<String> getQueryParamAsList(String key) {
        return getParameterValues(key)?.toList() ?: []
    }

    List<String> queryParams() {
        return getParameterMap().keySet().toList()
    }

    Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(
            getParameterMap().collectEntries {
                [(it.key): it.value.join(",")]
            }
        )
    }

    boolean hasQueryParams() {
        return !getParameterMap().isEmpty()
    }

    // ---------------- SESSION ----------------

    Session session() {
        String id = getCookies()?.find { it.name == SESSION_ID }?.value
            ?: UUID.randomUUID().toString()
        return new Session(id, getSession(true))
    }

    Cookie[] getCookies() {
        return servlet.getCookies()
    }

    // ---------------- BODY ----------------

    String body() {
        return getBody()
    }

    String getBody() {
        return Bytes.toString(getBodyAsBytes(), getCharacterEncoding() ?: "UTF-8")
    }

    byte[] getBodyAsBytes() {
        try {
            return IO.readBytes(getInputStream())
        } catch (Exception e) {
            Log.w("Exception when reading body", e)
            return new byte[0]
        }
    }

    String getUserAgent() {
        return getHeader("User-Agent")
    }
}