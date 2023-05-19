package com.intellisrc.web

import groovy.transform.CompileStatic

/**
 * @since 2023/05/19.
 */
@CompileStatic
interface Route {
    Object call(Request request, Response response)
}