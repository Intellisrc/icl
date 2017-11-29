package jp.sharelock.web

import org.pac4j.core.config.ConfigFactory
import spark.Request
import spark.Response

@groovy.transform.CompileStatic
/**
 * @since 17/04/04.
 */
class ServicePath {
    interface Action {
        Object run(Request request, Response response)
    }
    interface Allow {
        boolean check(Request request)
    }
    static enum Method {
        GET, POST, PUT, DELETE, OPTIONS
    }
    int cacheTime  = 0                                            // Seconds to store action in Cache
    boolean cacheExtend = false                                   // Extend time upon read (similar as sessions)
    String contentType = "application/json"
    String path    = ""                                           // URL path relative to parent
    Method method  = Method.GET                                   // HTTP Method to be used
    Action action  = { Request request -> null } as Action        // Closure that will return an Object (usually Map) to be converted to JSON as response
    Allow allow    = { Request request -> true } as Allow         // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    ConfigFactory config = null                                   // Specify configuration for security
}
