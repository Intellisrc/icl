package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * Wrapper for connection exceptions
 * @since 2026/01/28.
 */
@CompileStatic
class DatabaseConnectionException extends Exception {
    DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause)
    }
    DatabaseConnectionException(Throwable cause) {
        super(cause?.message, cause)
    }
}
