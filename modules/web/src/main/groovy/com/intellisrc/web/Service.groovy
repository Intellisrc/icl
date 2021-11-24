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
    static interface ActionResponse extends Action {
        Object run(Request request, Response response)
    }
    /**
     * Method to run on Upload, as parameter
     * is the temporally file uploaded
     * Its almost the same as Action but it includes the uploaded file
     */
    static interface Upload extends Action {
        Object run(UploadFile tmpFile)
    }
    static interface UploadRequest extends Action {
        Object run(UploadFile tmpFile, Request request)
    }
    static interface UploadResponse extends Action {
        Object run(UploadFile tmpFile, Request request, Response response)
    }
    /**
     * Method to run on Upload multiple files
     * similar to Upload, but returns a list
     */
    static interface Uploads extends Action {
        Object run(List<UploadFile> tmpFiles)
    }
    static interface UploadsRequest extends Action {
        Object run(List<UploadFile> tmpFiles, Request request)
    }
    static interface UploadsResponse extends Action {
        Object run(List<UploadFile> tmpFiles, Request request, Response response)
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
    static enum Method {
        GET, POST, PUT, DELETE, OPTIONS
    }
    boolean cacheExtend         = false                 // Extend time upon read (similar as sessions)
    boolean isPrivate           = false                 // Browser Rule: These responses are typically intended for a single user
    boolean noStore             = false                 // Browser Rule: If true, response will never cached (as it may contain sensitive information)
    int cacheTime               = 0                     // Seconds to store action in Server's Cache // 0 = "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER = forever
    int maxAge                  = 0                     // Seconds to suggest to keep in browser
    String contentType          = ""                    // Content Type, for example: Mime.getType("png") or "image/png". (default : auto)
    String charSet              = "UTF-8"               // Output charset (default: UTF-8)
    String path                 = ""                    // URL path relative to parent
    boolean download            = false                 // Specify if instead of display, show download dialog
    String downloadFileName     = ""                    // Use this name if download is requested
    Method method               = Method.GET            // HTTP Method to be used
    Object action               = { }                   // Closure that will return an Object (usually Map) to be converted to JSON as response
    Allow allow                 = { true } as Allow     // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    String allowOrigin          = null                  // By default only localhost is allowed to perform requests. This will set "Access-Control-Allow-Origin" header.
    Map<String,String> headers  = [:]                   // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
    ETag etag                   = { "" } as ETag        // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)
}
