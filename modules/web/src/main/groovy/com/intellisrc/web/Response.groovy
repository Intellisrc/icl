package com.intellisrc.web

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
                    bytes = Zip.brotliCompress(bytes)
                    break
                case GZIP_COMPRESSED:
                    bytes = Zip.gzip(bytes)
                    break
            }
            return obj
        }
    }
    Response(HttpChannel channel, HttpOutput out) {
        super(channel, out)
    }
    Compression compression = Compression.AUTO
    void status(int code) {
        //TODO
    }
    void redirect(String path) {
        //TODO
    }
    void type(String type) {
        //TODO
    }
    String type() {
        return "" //TODO
    }
    void header(String key, String value) {
        //TODO
    }
}
