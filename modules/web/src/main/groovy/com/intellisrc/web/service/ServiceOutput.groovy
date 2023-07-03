package com.intellisrc.web.service

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import static com.intellisrc.web.WebService.getDefaultCharset
import static com.intellisrc.web.service.Response.Compression
import static com.intellisrc.web.service.Response.Compression.AUTO

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
    Compression compression = AUTO

    // Used by URL
    int responseCode    = 0
    // Name used to download
    String fileName     = ""
    // Size of content
    long size           = 0
    // Store eTag in some cases
    String etag         = ""
    // Headers (extra headers, they may get override by other)
    final Map<String, String> headers = [:]

    @Override
    String toString() {
        if(type == Type.BINARY) {
            Log.w("Content Type is BINARY but 'toString()' was requested")
        }
        return content.toString()
    }

    /**
     * Import headers (e.g. from Services)
     * @param outHeaders
     */
    void importHeaders(Map<String, String> outHeaders) {
        headers.putAll(outHeaders)
    }
}
