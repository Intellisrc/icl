package com.intellisrc.web.service

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpStatus

/**
 * Default Exception for 'Service'
 * @since 2024/03/27.
 */
@CompileStatic
class WebException extends Exception {
    int code
    String text

    WebException(int code) {
        this(code, HttpStatus.getCode(code).message)
    }

    WebException(int code, Throwable cause) {
        this(code, HttpStatus.getCode(code).message, cause)
    }

    WebException(int code, String text, Throwable cause = null) {
        super(text, cause ?: new Exception(text))
        this.code = code
        this.text = text
    }

    @Override
    String getMessage() {
        return text + (cause?.message && cause?.message != text ? " : ${cause.message}" : "") ?: cause?.class?.simpleName ?: "Unknown"
    }
}
