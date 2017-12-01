package jp.sharelock.web

import spark.Request
import spark.Response

@groovy.transform.CompileStatic
/**
 * @since 17/04/04.
 */
class ServicePath {
    /**
     * Execute an action and return for example, JSON data
     */
    interface Action {
        Object run()
    }
    interface ActionRequest extends Action {
        Object run(Request request)
    }
    interface ActionRequestResponse extends Action {
        Object run(Request request, Response response)
    }
    /**
     * Check if client is allowed or not
     */
    interface Allow {
        boolean check(Request request)
    }
    /**
     * Method to run on Upload, as parameter
     * is the temporally file uploaded
     * Its almost the same as Action but it includes the uploaded file
     */
    interface Upload {
        Object run(File tmpFile)
    }
    interface UploadRequest extends Upload {
        Object run(File tmpFile, Request request)
    }
    interface UploadRequestResponse extends Upload {
        Object run(File tmpFile, Request request, Response response)
    }
    static enum Method {
        GET, POST, PUT, DELETE, OPTIONS
    }
    int cacheTime  = 0                                            // Seconds to store action in Cache
    boolean cacheExtend = false                                   // Extend time upon read (similar as sessions)
    String contentType = "application/json"
    String path    = ""                                           // URL path relative to parent
    String download = ""                                          // Specify if instead of display, show download dialog with the name of the file.
    Method method  = Method.GET                                   // HTTP Method to be used
    Action action  = { } as Action                                // Closure that will return an Object (usually Map) to be converted to JSON as response
    Allow allow    = { true } as Allow                            // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    String uploadField = "upload"                                 // Name of the HTML input[type=file]
    Upload upload  = null                                         // When uploading files to server, use this parameter
}
