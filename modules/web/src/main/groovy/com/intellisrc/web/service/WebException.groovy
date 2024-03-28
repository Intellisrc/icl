package com.intellisrc.web.service

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

/**
 * Default Exception for 'Service'
 * @since 2024/03/27.
 */
@CompileStatic
@TupleConstructor
class WebException extends Exception {
    int code
    String text
}
