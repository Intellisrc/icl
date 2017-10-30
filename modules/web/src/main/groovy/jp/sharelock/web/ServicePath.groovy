package jp.sharelock.web

import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/04.
 */
class ServicePath {
    interface Action {
        Object run(Request request)
    }
    interface Allow {
        boolean check(Request request)
    }
    static enum Method {
        GET, POST, PUT, DELETE, OPTIONS
    }
    String fullPath= ""                                           // To be filled by the application
    int cacheTime  = 0                                            // Seconds to store action in Cache
    Method method  = Method.GET                                   // HTTP Method to be used
    String path    = ""                                           // URL path relative to parent
    Action action  = { Request request -> null } as Action        // Closure that will return an object (usually Collection or HashMap) to be converted to JSON as response
    Allow allow    = { Request request -> true } as Allow         // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
}
