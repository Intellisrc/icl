package com.intellisrc.web.service

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.eclipse.jetty.http.HttpMethod

/**
 * Define a route. Used to keep available routes in memory
 */
@CompileStatic
@TupleConstructor
class RouteDefinition {
    HttpMethod method = HttpMethod.GET
    String path = "/"
    String acceptType = "*/*"
    Route action = { Request request, Response response -> } as Route
    String getPath() {
        return path.startsWith("/") ? path : '/' + path
    }
}
