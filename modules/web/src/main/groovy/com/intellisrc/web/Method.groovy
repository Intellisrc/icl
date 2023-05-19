package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * HTTP methods
 */
@CompileStatic
enum Method {
    HEAD, GET, POST, PUT, DELETE, OPTIONS, PATCH, CONNECT, TRACE
}