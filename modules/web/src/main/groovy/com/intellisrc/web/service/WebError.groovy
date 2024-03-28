package com.intellisrc.web.service

import com.intellisrc.etc.JSON
import com.intellisrc.etc.Mime
import com.intellisrc.etc.YAML
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

import static com.intellisrc.web.WebService.getDefaultCharset

/**
 * Class to specify HTTP error display
 * @since 2024/03/27.
 */
@CompileStatic
@TupleConstructor
class WebError {
    String content = ""
    String contentType = ""
    String charSet = ""

    static interface WebErrorTemplate {
        WebError call(int code, String msg, String contentType)
    }

    static WebErrorTemplate getDefaultErrorTemplate() {
        return {
            int code, String msg, String contentType ->
                return switch (contentType) {
                    case Mime.JSON  -> new WebError(JSON.encode([ok: false, error: code, msg: msg]), contentType, defaultCharset)
                    case Mime.YAML  -> new WebError(YAML.encode([ok: false, error: code, msg: msg]), contentType, defaultCharset)
                    case Mime.HTML  -> new WebError(String.format("<html><head><title>Error %d</title></head><body><h1>Error %d</h1><hr><p>%s</p></body></html>", code, code, msg), contentType, defaultCharset)
                    default         -> new WebError(String.format("Error %d : %s", code, msg), Mime.TXT, defaultCharset)
                }
        }
    }
}
