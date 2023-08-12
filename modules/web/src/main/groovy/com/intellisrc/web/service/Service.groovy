package com.intellisrc.web.service

import com.intellisrc.etc.Cache
import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import static com.intellisrc.web.WebService.getDefaultCharset
import static com.intellisrc.web.service.HttpHeader.ACCEPT_CONTROL_ALLOW_ORIGIN
import static com.intellisrc.web.service.HttpHeader.CACHE_CONTROL

/**
 * @since 17/04/04.
 */
@CompileStatic
class Service implements Serviciable {
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
    boolean isPrivate           = false                 // Browser Rule: These responses are typically intended for a single user
    boolean noStore             = false                 // Browser Rule: If true, response will never cached (as it may contain sensitive information)
    boolean compress            = false                 // Whether to compress or not the output (defaults to WebService value, which is true by default)
    boolean cacheExtend         = false                 // Extend time upon read (similar as sessions)
    int minCompressBytes        = 256                   // Below this length, do not compress (most probably there won't be any gain)
    int cacheTime               = Cache.DISABLED        // Seconds to store action in Server's Cache // 0 = "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER = forever
    int maxAge                  = 0                     // Seconds to suggest to keep in browser
    String contentType          = ""                    // Content Type, for example: Mime.getType("png") or "image/png". (default : auto)
    String charSet              = defaultCharset        // Output charset (default: UTF-8)
    String path                 = ""                    // URL path relative to parent
    boolean download            = false                 // Specify if instead of display, show download dialog
    String downloadFileName     = ""                    // Use this name if download is requested
    HttpMethod method           = HttpMethod.GET        // HTTP Method to be used
    Object action               = { }                   // Closure that will return an Object (usually Map) to be converted to JSON as response
    Allow allow                 = { true } as Allow     // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    String allowOrigin          = null                  // By default only localhost is allowed to perform requests. This will set "Access-Control-Allow-Origin" header.
    String acceptType           = ""                    // By default it accepts all mime types, but you can set to accept only specific types like `application/json` (default `*/*`)
    Map<String,String> headers  = new TreeMap<>(String.CASE_INSENSITIVE_ORDER) // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
    ETag etag                   = { "" } as ETag        // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)

    // The following are used by WebService to set correctly the users intention with compression:
    protected boolean compressIsExplicit = false
    void setCompress(boolean val) {
        compressIsExplicit = true
        compress = val
    }
    boolean getCompress(boolean generalValue) {
        return compressIsExplicit ? compress : generalValue
    }
    /**
     * Default set path (as String)
     * @param path
     */
    void setPath(String path) {
        boolean isRegex = ["\\","(","{","[","^","\$"].any { path.contains(it) }
        this.path = (isRegex ? "~" + (path.startsWith("/") ? "" : "/") : "") + addRoot(path) // Trailing slash is optional
    }
    /**
     * Optional method to use Pattern as path
     * @param pattern
     */
    /*void setPath(Pattern pattern) {
        this.path = "~/" + addRoot(pattern.toString()) + "/"
    }*/

    /**
     * Generate headers according to restraints
     * @return
     */
    Map<String, String> getHeaders() {
        Map<String, String> autoHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
        if (allowOrigin) {
            autoHeaders.put(ACCEPT_CONTROL_ALLOW_ORIGIN, allowOrigin)
        }
        if (noStore) { //Never store in client
            autoHeaders.put(CACHE_CONTROL, "no-store")
        } else if (!cacheTime && !maxAge) { //Revalidate each time
            autoHeaders.put(CACHE_CONTROL, "no-cache")
        } else {
            String priv = (isPrivate) ? "private," : "" //User-specific data
            autoHeaders.put(CACHE_CONTROL, priv + "max-age=" + maxAge) //IDEA: when using server cache, synchronize remaining time in cache with this value
        }
        autoHeaders.putAll(headers)
        return autoHeaders
    }

    /**
     * Will modify the regex to add the starting slash if it is not present
     * @param regex
     * @return
     */
    private static String addRoot(String regex) {
        if(regex.startsWith("^") &&! regex.startsWith("^/")) {
            regex = regex.replaceFirst("\\^","^/")
        }
        return regex
    }

    static Service 'new'(HttpMethod method, String path, Object action, String acceptType = "*/*") {
        return new Service(
            method      : method,
            path        : path,
            action      : action,
            acceptType  : acceptType
        )
    }
}
