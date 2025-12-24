package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Bytes
import groovy.transform.CompileStatic
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.Part
import jakarta.websocket.Session as JakartaSession
import jakarta.websocket.server.HandshakeRequest
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.util.IO

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
class Request {

    static final String X_FORWARDED_FOR = "X-Forwarded-For"
    static final String SESSION_ID = "JSESSIONID"

    /** Servlet API request */
    final HttpServletRequest servlet
    final HandshakeRequest handshake
    final HttpSession httpSession
    protected JakartaSession wsSession

    protected final InetSocketAddress webSocketAddress

    final ConcurrentHashMap<String, String> pathParameters = new ConcurrentHashMap<>()
    protected String splat = ""

    Request(ServletRequest request) {
        this.servlet = (HttpServletRequest) request
        this.handshake = null
        this.webSocketAddress = null
        this.httpSession = this.servlet.session
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Not an HTTP request")
        }
    }

    Request(HandshakeRequest handshake, InetSocketAddress address, HttpSession httpSession, HttpServletRequest servletRequest) {
        this.handshake = handshake
        this.webSocketAddress = address
        this.servlet = servletRequest
        this.httpSession = httpSession

        if (!(handshake instanceof HandshakeRequest)) {
            throw new IllegalArgumentException("Not a WS handshake request")
        }
    }

    // ---------------- IP / HOST ----------------

    String ip() { getIp() }

    String getIp() {
        String forwarded = getHeader(X_FORWARDED_FOR)
        return forwarded ?: handshake ? webSocketAddress.address.hostAddress : servlet.getRemoteAddr()
    }

    String uri() {
        return handshake ? handshake.getRequestURI() : servlet?.getRequestURI()
    }

    String host() { getHost() }

    String getHost() {
        String local = handshake ? webSocketAddress.hostName : servlet?.getLocalName()
        if (!(local =~ /[a-zA-Z]+/)) {
            try {
                local.toInetAddress()
                local = getHostHeader().tokenize(":").first()
            } catch (Exception ignore) {}
        }
        return local
    }

    String getAddress() {
        return servlet?.getLocalAddr()
    }

    String getHostHeader() {
        return getHeader("Host") ?: "localhost"
    }

    int getPort() {
        return handshake ? webSocketAddress.port : servlet.getLocalPort()
    }

    String scheme() {
        return handshake ? "ws" : servlet.getScheme()   //TODO: Do we need to return ws / wss?
    }

    // ---------------- HEADERS / ATTRIBUTES ----------------

    String getMethod() {
        return handshake ? "GET" : servlet.method // FIXME: method for WS
    }

    String getHeader(String key) {
        return handshake ? handshake.headers[key].join(",") : servlet.getHeader(key)
    }

    String headers(String key) {
        return getHeader(key)
    }

    String attribute(String key) {
        if(handshake) {
            Log.w("Websockets has no attributes")
            return ""
        }
        return (String) servlet.getAttribute(key)
    }

    void attribute(String key, Object value) {
        if(handshake) {
            Log.w("Websockets has no attributes")
            return
        }
        servlet.setAttribute(key, value)
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

    String getQueryString() {
        return handshake ? handshake.queryString : servlet?.queryString
    }

    String queryParams(String key) {
        return getQueryParam(key)
    }

    String getQueryParam(String key) {
        return handshake ? handshake.queryString.queryMap[key].toString() : servlet.getParameter(key)
    }

    //FIXME: in case of handshake, I'm not sure how an array will be handled in the query string (it is very unlikely that it will be used)
    List<String> getQueryParamAsList(String key) {
        return handshake ? handshake.queryString.queryMap[key].toString().tokenize(",") : servlet.getParameterValues(key)?.toList() ?: []
    }

    List<String> queryParams() {
        return handshake ? handshake.queryString.queryMap.keySet()?.toList() : servlet.getParameterMap().keySet().toList()
    }

    Map<String, String> getQueryParams() {
        return (handshake ? handshake.queryString.queryMap: Collections.unmodifiableMap(
            servlet.getParameterMap().collectEntries {
                [(it.key): it.value.join(",")]
            }
        )) as Map<String, String>
    }

    boolean hasQueryParams() {
        return ! (handshake ? handshake.queryString.replace('?','').trim().empty : servlet.getParameterMap().isEmpty())
    }

    // ---------------- SESSION ----------------
    void setWebSocket(JakartaSession session) {
        wsSession = session
    }

    // FIXME: this session won't contain websocket session
    Session session() {
        String id = handshake ?
            wsSession.id :
            getCookies()?.find { it.name == SESSION_ID }?.value ?: UUID.randomUUID().toString()
        return handshake ? new Session(id, wsSession) : new Session(id, servlet.getSession(true))
    }

    Session getSession() {
        return session()
    }

    Cookie[] getCookies() {
        return (handshake ? [] : servlet?.getCookies()) as Cookie[]
    }

    // ---------------- BODY ----------------

    long getContentLength() {
        return servlet?.contentLength ?: 0
    }

    String body() {
        return getBody()
    }

    String getBody() {
        return Bytes.toString(getBodyAsBytes(), servlet.getCharacterEncoding() ?: "UTF-8")
    }

    Collection<Part> getParts() {
        return servlet?.parts ?: []
    }

    byte[] getBodyAsBytes() {
        if(handshake) {
            Log.w("WebSockets has no body")
            return new byte[0]
        }
        try {
            return IO.readBytes(servlet.getInputStream())
        } catch (Exception e) {
            Log.w("Exception when reading body", e)
            return new byte[0]
        }
    }

    String getUserAgent() {
        return headers("User-Agent")
    }
}