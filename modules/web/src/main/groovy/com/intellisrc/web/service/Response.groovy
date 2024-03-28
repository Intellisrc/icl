package com.intellisrc.web.service

import com.intellisrc.core.Log
import jakarta.servlet.ServletOutputStream
import org.eclipse.jetty.server.Response as JettyResponse

import java.lang.reflect.Field

import static com.intellisrc.web.service.WebError.*

/**
 * @since 2023/05/19.
 */
class Response extends JettyResponse {
    protected final JettyResponse original
    WebErrorTemplate errorTemplate = null
    Compression compression = Compression.NONE
    /**
     * Constructor
     * @param channel
     * @param out
     */
    Response(JettyResponse response) {
        super(response.httpChannel, response.httpOutput)
        original = response
        JettyResponse.class.declaredFields.each {
            Field field ->
                try {
                    field.setAccessible(true)
                    Object value = field.get(response)
                    field.set(this, value)
                } catch (Exception ignore) {
                    // Handle the exception as needed
                }
        }
    }

    /**
     * Export a Response into Jetty
     * @return
     */
    void update() {
        JettyResponse.class.declaredFields.each {
            Field field ->
                try {
                    field.setAccessible(true)
                    Object value = field.get(this)
                    field.set(original, value)
                } catch (Exception ignore) {
                    // Handle the exception as needed
                }
        }
        // Copy headers
        headers.each {
            original.setHeader(it.key, it.value)
        }
    }

    @Override
    PrintWriter getWriter() {
        return original.writer
    }
    @Override
    ServletOutputStream getOutputStream() {
        return original.outputStream
    }
    /**
     * Get length
     * @return
     */
    int getLength() {
        return (header("Content-Length") ?: "0") as int
    }
    /**
     * Set status
     * @param code
     */
    void status(int code) {
        original.status = code
        setStatus(code)
    }
    /**
     * Redirect
     * @param path
     */
    void redirect(String path) {
        original.sendRedirect(path)
        sendRedirect(path)
    }
    /**
     * Set content-type
     * @param type
     */
    void type(String type) {
        original.setContentType(type)
        setContentType(type)
    }
    /**
     * Get content-type
     * @return
     */
    String type() {
        return getContentType()
    }
    /**
     * Set header
     * @param key
     * @param value
     */
    void header(String key, String value) {
        if(key != "Date") {
            if (headerNames.contains(key) && value != header(key)) {
                Log.v("HTTP Header: '%s' already existed. Replaced: %s -> %s", key, header(key), value)
            }
            original.setHeader(key, value)
            setHeader(key, value)
        }
    }
    /**
     * Get header
     * @param key
     * @return
     */
    String header(String key) {
        return getHeader(key)
    }
    /**
     * Get a copy of all headers
     * @return
     */
    Map<String, String> getHeaders() {
        Map<String, String> insensitiveMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
        insensitiveMap.putAll(headerNames.findAll {
            header(it) != null && header(it) != ""
        }.collectEntries {[ (it): header(it) ] })
        return Collections.unmodifiableMap(insensitiveMap)
    }
}
