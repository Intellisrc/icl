package com.intellisrc.web.service

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import static com.intellisrc.web.WebService.getDefaultCharset
import static org.eclipse.jetty.http.HttpStatus.NOT_MODIFIED_304

/**
 * Customized output of a Service.
 * You can use this class when more flexibility is needed
 * in the return value of a Service (to be processed as Response)
 *
 * It was originally part of WebService, but was moved out to
 * enhance its functionality
 *
 * @since 2022/05/26.
 */
@CompileStatic
class ServiceOutput {
    /**
     * Output types used in getOutput
     */
    static enum Type {
        JSON, YAML, TEXT, IMAGE, BINARY, STREAM
        /**
         * From content type, get OutputType
         * @param contentType
         * @return
         */
        static Type fromString(String contentType) {
            Type type
            //noinspection GroovyFallthrough
            switch (contentType) {
                case ~/.*json.*/  : type = JSON; break
                case ~/.*yaml.*/  : type = YAML; break
                case ~/.*text.*/  : type = TEXT; break
                case ~/.*image.*/ : type = IMAGE; break
                case ~/.*stream.*/ : type = STREAM; break
                default:
                    type = BINARY
            }
            return type
        }
    }
    Type type           = Type.BINARY
    Object content      = null
    String contentType  = ""
    String charSet      = defaultCharset
    String downloadName = ""
    Compression compression = Compression.available.first()

    // Used by URL
    int responseCode    = 0
    // Name used to download
    String fileName     = ""
    // Size of content
    int size           = 0
    // Store eTag in some cases
    String etag         = ""
    // Headers (extra headers, they may get override by other)
    final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER)

    @Override
    String toString() {
        if(type == Type.BINARY) {
            Log.w("Content Type is BINARY but 'toString()' was requested with content-type: %s", contentType)
        }
        return content.toString()
    }

    /**
     * Import headers (e.g. from Services)
     * only if it is not set already
     * @param outHeaders
     */
    void importHeaders(Map<String, String> outHeaders) {
        outHeaders.each {
            headers.putIfAbsent(it.key, it.value)
        }
    }
    /**
     * If output was not modified, reset
     */
    void setNotModified() {
        headers.clear()
        responseCode = NOT_MODIFIED_304
        compression = Compression.NONE
        size = 0
        content = null
        contentType = null
    }
}
