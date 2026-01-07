package com.intellisrc.web.service

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.eclipse.jetty.http.HttpStatus

/**
 * Default Exception for 'Service'
 * @since 2024/03/27.
 */
@CompileStatic
class WebException extends Exception {
    final int code
    final Serviciable service
    final String text

    WebException(int code) {
        this(code, "")
    }
    WebException(int code, Throwable cause) {
        this(code, "", cause)
    }
    WebException(int code, String text, Throwable cause = null) {
        this(null, code, text, cause)
    }
    WebException(Serviciable sp, int code) {
        this(sp, code, "")
    }
    WebException(Serviciable sp, int code, Throwable cause) {
        this(sp, code, "", cause)
    }

    WebException(Serviciable sp, int code, String text, Throwable cause = null) {
        super(text, cause ?: new Exception(text))
        if(cause instanceof WebException) {
            this.service = cause.service
            this.code = cause.code
            this.text = cause.text
        } else {
            this.service = sp
            this.code = code
            this.text = text ?: HttpStatus.getCode(code).message
        }
    }

    @Override
    String getMessage() {
        return text + (cause?.message && cause?.message != text ? " : ${cause.message}" : "") ?: cause?.class?.simpleName ?: "Unknown"
    }
}
