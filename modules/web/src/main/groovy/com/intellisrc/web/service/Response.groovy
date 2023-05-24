package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Zip
import org.eclipse.jetty.server.HttpChannel
import org.eclipse.jetty.server.HttpOutput
import org.eclipse.jetty.server.Response as JettyResponse

import java.lang.reflect.InvocationTargetException

/**
 * @since 2023/05/19.
 */
class Response extends JettyResponse {
    private static final String CONTENT_ENCODING = "Content-Encoding"
    Compression compression = Compression.AUTO
    /**
     * Handles the output compression
     */
    static enum Compression {
        AUTO, BROTLI_COMPRESSED, GZIP_COMPRESSED, NONE
        boolean isAvailable() {
            boolean available = true
            if(this == BROTLI_COMPRESSED) {
                try {
                    Class<?> brotli = Class.forName("com.nixxcode.jvmbrotli.common.BrotliLoader")
                    available = (Boolean) brotli.getMethod("isBrotliAvailable").invoke(null)
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.d("Brotli was not found: {} Cause: {}", e.getMessage(), e.getCause());
                    available = false
                }
            }
            return available
        }
        Compression get() {
            return this == AUTO ? (BROTLI_COMPRESSED.isAvailable() ? BROTLI_COMPRESSED : GZIP_COMPRESSED) : this
        }
        Object compress(byte[] bytes) {
            Object obj = bytes
            switch(get()) {
                case BROTLI_COMPRESSED:
                    obj = Zip.brotliCompress(bytes)
                    break
                case GZIP_COMPRESSED:
                    obj = Zip.gzip(bytes)
                    break
                default:
                    break
            }
            return obj
        }
        void setHeader(Response response) {
            switch(get()) {
                case BROTLI_COMPRESSED:
                    response.setHeader(CONTENT_ENCODING, "br")
                    break
                case GZIP_COMPRESSED:
                    response.setHeader(CONTENT_ENCODING, "gzip")
                    break
                default:
                    response.setHeader(CONTENT_ENCODING, "")
                    break
            }
        }
    }
    /**
     * Constructor
     * @param channel
     * @param out
     */
    Response(HttpChannel channel, HttpOutput out) {
        super(channel, out)
    }
    /**
     * Import a "Response object from Jetty"
     * @param response
     * @return
     */
    static Response 'import'(JettyResponse response) {
        Response newResponse = new Response(response.httpChannel, response.httpOutput)
        JettyResponse.class.declaredFields.each {
            try {
                it.setAccessible(true)
                Object value = it.get(response)
                it.set(newResponse, value)
            } catch (IllegalAccessException ignore) {
                // Handle the exception as needed
            }
        }
        return newResponse
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
        setStatus(code)
    }
    /**
     * Redirect
     * @param path
     */
    void redirect(String path) {
        sendRedirect(path)
    }
    /**
     * Set content-type
     * @param type
     */
    void type(String type) {
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
        setHeader(key, value)
    }
    /**
     * Get header
     * @param key
     * @return
     */
    String header(String key) {
        return getHeader(key)
    }
}
