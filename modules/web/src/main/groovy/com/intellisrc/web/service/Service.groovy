package com.intellisrc.web.service

import com.intellisrc.core.Log
import com.intellisrc.etc.Cache
import com.intellisrc.web.service.routing.PathMatcher
import com.intellisrc.web.service.routing.ExactMatcher
import com.intellisrc.web.service.routing.GlobMatcher
import com.intellisrc.web.service.routing.OptionalMatcher
import com.intellisrc.web.service.routing.ParamsMatcher
import com.intellisrc.web.service.routing.RegExMatcher
import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import java.util.regex.Pattern

import static com.intellisrc.web.WebService.getDefaultCharset
import static com.intellisrc.web.service.HttpHeader.ACCEPT_CONTROL_ALLOW_ORIGIN
import static com.intellisrc.web.service.HttpHeader.CACHE_CONTROL

/**
 * @since 17/04/04.
 */
@CompileStatic
class Service implements Serviciable {
    static enum ServiceType {
        HTTP, SSE, WS
        String getProtocol() {
            return switch (this) {
                case WS -> "ws"
                default -> "http"
            }
        }
        boolean getProcessService() {
            return switch (this) {
                case HTTP -> true
                default -> false
            }
        }
    }
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
     * Used to pass errors to client
     * it should return `true` to disable default action (log)
     */
    static interface ServiceError {
        boolean call(WebException we)
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
     * Executed before request is passed to the Service
     */
    static interface BeforeRequest {
        void run(Request request)
    }
    /**
     * Executed before the response is handled to WebService
     */
    static interface BeforeResponse {
        void run(Response response)
    }

    /**
     * Re-scopes the closure/interface so 'delegate' refers to this Service instance
     */
    private Object wire(Object candidate) {
        if (candidate instanceof Closure) {
            Closure cloned = (Closure) candidate.clone()
            cloned.delegate = this
            // DELEGATE_FIRST allows you to access Service properties
            // (like 'path' or 'cacheTime') directly inside the closure
            cloned.resolveStrategy = Closure.DELEGATE_FIRST
            return cloned
        }
        return candidate
    }
    // Closures to bind 'delegate' to them (so you can call delegate to refer to the Service instance):
    private Object _action                      = {}
    //Object action               = { }                 // Closure that will return an Object (usually Map) to be converted to JSON as response

    void setAction(@DelegatesTo(Service) Object cl) { _action = wire(cl) }
    Object getAction() { _action }

    boolean isPrivate           = false                 // Server Rule: These responses are typically intended for a single user
    boolean noStore             = false                 // Server Rule: If true, response will never cached (as it may contain sensitive information)
    boolean compress            = false                 // Whether to compress or not the output (defaults to WebService value, which is true by default)
    boolean strictPath          = false                 // If true, regex should also match starting '/' character. For example: ~/^\/hello.html?/ instead of: ~/hello.html?/
    int minCompressBytes        = 256                   // Below this length, do not compress (most probably there won't be any gain)
    int cacheTime               = Cache.DISABLED        // Seconds to store action in Server's Cache // 0 = "no-cache" Browser Rule: If true, the client must revalidate ETag to decide if download or not. Cache.FOREVER = forever
    int maxAge                  = 0                     // Seconds to suggest to keep in browser
    String contentType          = ""                    // Content Type, for example: Mime.getType("png") or "image/png". (default : auto)
    String charSet              = defaultCharset        // Output charset (default: UTF-8)
    String path                 = ""                    // URL path relative to parent
    boolean download            = false                 // Specify if instead of display, show download dialog
    String downloadFileName     = ""                    // Use this name if download is requested
    HttpMethod method           = HttpMethod.GET        // HTTP Method to be used
    Allow allow                 = null                  // By default will allow everyone. If a Closure is set, it will be evaluated if the request is allowed or not
    String notAllowedRedirect   = ""                    // If set it will redirect not allowed requests to that URL or path
    String allowOrigin          = null                  // By default only localhost is allowed to perform requests. This will set "Access-Control-Allow-Origin" header.
    String acceptType           = ""                    // By default it accepts all mime types, but you can set to accept only specific types like `application/json` (default `*/*`)
    String acceptCharset        = ""                    // By default it accepts all charsets
    Map<String,String> headers  = new TreeMap<>(String.CASE_INSENSITIVE_ORDER) // Extra headers to the response. e.g. : "Access-Control-Allow-Origin" : "*"
    ETag etag                   = { "" } as ETag        // Method to calculate ETag if its different from default (set it to null, to disable automatic ETag)
    // Hooks:
    BeforeRequest beforeRequest     = null
    BeforeResponse beforeResponse   = null
    ServiceError onError = null                         // { int code, Exception original -> } : Used to pass errors on actions

    // Internally used to setup special rules (automatically assigned)
    ServiceType serviceType = ServiceType.HTTP
    // Used for path matching
    protected PathMatcher matcher   = null
    List<String> samplePaths = []                       // Required for regular expression matching (example URI paths that should match that RegExp)

    PathMatcher getMatcher() {
        if (!matcher) {
            // Automatically assign Matcher:
            matcher = switch (true) {
                case path.startsWith("~/") -> new RegExMatcher()
                case path.contains("/:") -> new ParamsMatcher()
                case path.endsWith("/?") -> new OptionalMatcher()
                case path.endsWith("*") -> new GlobMatcher()
                default -> new ExactMatcher()
            }
        }
        if(matcher.pathEmpty) {
            matcher.path = path
        }
        matcher.samples.addAll(samplePaths)

        if(matcher instanceof RegExMatcher && matcher.samples.empty) {
            Log.w("Regular expression paths may collide with other paths, " +
                "so it is recommended to set 'samplePaths' property for path: %s", path)
        }
        return matcher
    }
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
        if(isRegex) {
            setPath(toPattern(path))
        } else {
            this.path = path // Trailing slash is optional
        }
    }

    /**
     * Optional method to use Pattern as path
     * @param pattern
     */
    void setPath(Pattern pattern) {
        this.path = "~/" + pattern.toString() + "/"
    }

    /**
     * Checks if two services path collide
     * @param a
     * @param other
     * @return
     */
    boolean collides(Service other) {
        return (method == other.method) &&
            (serviceType.protocol == other.serviceType.protocol) &&
            (acceptCharset == other.acceptCharset) &&
            (acceptType == other.acceptType) &&
            matcher.overlaps(other.matcher)
    }
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

    static Service 'new'(HttpMethod method, String path, Object action, String acceptType = "*/*") {
        return new Service(
            method      : method,
            path        : path,
            action      : action,
            acceptType  : acceptType
        )
    }

    static Pattern toPattern(String path) {
        return Pattern.compile(path.replaceFirst(/^~/, '').replaceFirst(/^\//, '').replaceFirst(/\/$/,''), Pattern.CASE_INSENSITIVE)
    }
}
