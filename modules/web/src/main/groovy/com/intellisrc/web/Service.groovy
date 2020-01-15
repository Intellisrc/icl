package com.intellisrc.web

import groovy.transform.CompileStatic
import spark.Request
import spark.Response

/**
 * @since 17/04/04.
 */
@CompileStatic
class Service {
    /**
     * Execute an action and return for example, JSON data
     */
    static interface Action {
        Object run()
    }
    static interface ActionRequest extends Action {
        Object run(Request request)
    }
    static interface ActionRequestResponse extends Action {
        Object run(Request request, Response response)
    }
    /**
     * Check if client is allowed or not
     */
    static interface Allow {
        boolean check(Request request)
    }
    /**
     * Return the ETag of the output
     */
    static interface ETag {
        String calc(Object out)
    }
    /**
     * Method to run on Upload, as parameter
     * is the temporally file uploaded
     * Its almost the same as Action but it includes the uploaded file
     */
    static interface Upload {
        Object run(File tmpFile)
    }
    static interface UploadRequest extends Upload {
        Object run(File tmpFile, Request request)
    }
    static interface UploadRequestResponse extends Upload {
        Object run(File tmpFile, Request request, Response response)
    }
    static enum Method {
        GET, POST, PUT, DELETE, OPTIONS
    }
    boolean cacheExtend         = false                 // Extend time upon read (similar as sessions)
    boolean isPrivate           = false                 // Browser Rule: These responses are typically intended for a single user
    boolean noStore             = false                 // Browser Rule: If true, response will never cached (as it may contain sensitive information)
    int cacheTime               = 0                     // Seconds to store action in Server's Cache // 0 = "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER = forever
    int maxAge                  = 0                     // Seconds to suggest to keep in browser
    String contentType          = "application/json"
    String path                 = ""                    // URL path relative to parent
    String download             = ""                    // Specify if instead of display, show download dialog with the name of the file.
    Method method               = Method.GET            // HTTP Method to be used
    Action action               = { } as Action         // Closure that will return an Object (usually Map) to be converted to JSON as response
    Allow allow                 = { true } as Allow     // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    String uploadField          = "upload"              // Name of the HTML input[type=file]
    Upload upload               = null                  // When uploading files to server, use this parameter
    Map<String,String> headers  = [:]                   // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
    ETag etag                   = { "" } as ETag        // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)
}
