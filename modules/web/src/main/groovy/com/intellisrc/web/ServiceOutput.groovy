package com.intellisrc.web

import groovy.transform.CompileStatic
import groovy.transform.Immutable

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
    public static enum Type {
        JSON, YAML, TEXT, IMAGE, BINARY
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
                default:
                    type = BINARY
            }
            return type
        }
    }
    Type type           = Type.BINARY
    Object content      = null
    String contentType  = ""
    String charSet      = "UTF-8"

    // Used by URL
    int responseCode    = 0
    // Name used to download
    String fileName     = ""
    // Size of content
    long size           = 0
    // Store eTag in some cases
    String etag         = ""
}
