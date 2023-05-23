package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.etc.Cache
import com.intellisrc.web.protocols.Protocol
import groovy.transform.CompileStatic
import groovy.transform.RecordType
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Server as JettyServer
import org.eclipse.jetty.util.thread.QueuedThreadPool

import java.nio.ByteBuffer
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @since 2023/05/19.
 */
@CompileStatic
class Server implements Handler {
    static final String ACCEPT_TYPE_REQUEST_MIME_HEADER = "Accept"
    static final String CONTENT_TYPE_HEADER = "Content-Length"
    static final Cache<byte[]> staticCache = new Cache<>(timeout: Cache.FOREVER)

    /**
     * Define a route. Used to keep available routes in memory
     */
    @RecordType
    static protected class RouteDefinition {
        HttpMethod method = HttpMethod.GET
        String path = "/"
        String acceptType = "*/*"
        Route action = { Request request, Response response -> } as Route
        String getPath() {
            return path.startsWith("/") ? path : '/' + path
        }
    }
    /**
     * Result of a match filter.
     * As parameters are extracted during filtering
     * they are included here
     */
    @RecordType
    static protected class MatchFilterResult {
        Optional<RouteDefinition> route
        Map<String, String> params
    }

    final int port
    final int idleTimeout
    final boolean multiThread
    final Inet4Address bindAddress
    final KeyStore certificate
    final ConcurrentLinkedQueue<RouteDefinition> definitions = new ConcurrentLinkedQueue<>()
    final List<String> indexFiles = []
    final File logFile
    protected boolean running = false
    protected int cacheMaxSizeKB = 0
    protected String staticPath = ""
    protected JettyServer jettyServer
    /**
     * Prepare server
     * @param bindAddress
     * @param port
     * @param protocol
     * @param cert
     * @param maxThreads
     * @param minThreads
     * @param idleTimeout
     */
    Server(Inet4Address bindAddress, int port, Protocol protocol, KeyStore cert, int maxThreads, int minThreads, int idleTimeout, File logFile) {
        this.bindAddress = bindAddress
        this.port = port
        this.idleTimeout = idleTimeout
        this.certificate = cert?.valid ? cert : null
        this.logFile = logFile
        if(cert &&! secure) {
            Log.w("KeyStore is invalid. Not using SSL.")
        }
        if(minThreads > maxThreads) { minThreads = maxThreads }
        this.multiThread = maxThreads > 0
        jettyServer = multiThread ? new JettyServer(new QueuedThreadPool(maxThreads, minThreads, idleTimeout)) : new JettyServer()
        jettyServer.addConnector(protocol.get(this).connector)
        jettyServer.setHandler(this)
    }
    /**
     * Specify static path and cache rules
     * @param path
     * @param expirationSec
     * @param cacheMaxSizeKB
     * @return
     */
    Server setStaticPath(String path, int expirationSec, int cacheMaxSizeKB) {
        this.staticPath = path
        this.cacheMaxSizeKB = cacheMaxSizeKB
        if(expirationSec) {
            staticCache.timeout = expirationSec
        }
        return this
    }

    /**
     * Process the path filter. Here we decide what to serve.
     * If we match a Service, we execute its action, otherwise we
     * locate a static file or return 404
     * IDEA: we can add here file restrictions to prevent serving specific files
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     */
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Request request = servletRequest as Request
        Response response = servletResponse as Response
        MatchFilterResult mfr = matchURI(request.requestURI, HttpMethod.fromString(request.method.trim().toUpperCase()), request.headers(ACCEPT_TYPE_REQUEST_MIME_HEADER))
        Object out = null
        if(mfr.route.present) {
            request.setPathParameters(mfr.params)   // Inject params to request
            out = mfr.route.get().action.call(request, response)
        } else {
            // The request is already clean from Jetty and without query string:
            String uri = request.requestURI
            if(uri &&! uri.empty) {
                List<String> options = uri.endsWith("/") ?
                    indexFiles.collect { uri + it } : [uri]
                options.any {
                    File staticFile = File.get(staticPath, it)
                    //noinspection GrReassignedInClosureLocalVar
                    out = staticCache.get(uri, {
                        staticFile.exists() && (staticFile.size() / 1024 <= cacheMaxSizeKB) ? staticFile.bytes : null
                    })
                    if (out == null && staticFile.exists()) {
                        //noinspection GrReassignedInClosureLocalVar
                        out = staticFile.bytes
                    }
                    return out != null
                }
            } else { // Very unlikely that will end up here:
                Log.w("Invalid request (empty)")
                response.status(HttpStatus.BAD_REQUEST_400)
            }
        }
        if (out != null) {
            if (!response.committed) {
                OutputStream responseStream = response.outputStream
                //noinspection GroovyFallthrough
                switch (out) {
                    case String:
                        String text = out.toString()
                        if(! response.getHeader(CONTENT_TYPE_HEADER)) {
                            response.header(CONTENT_TYPE_HEADER, sprintf("%d", text.length()))
                        }
                        responseStream.write(text.getBytes("utf-8")) //FIXME: not always will be UTF-8
                        break
                    case ByteBuffer:
                        out = (out as ByteBuffer).array()
                    case byte[]:
                        if(! response.getHeader(CONTENT_TYPE_HEADER)) {
                            response.header(CONTENT_TYPE_HEADER, sprintf("%d", (out as byte[]).length))
                        }
                        responseStream.write(out as byte[])
                        break
                    case InputStream:
                        (out as InputStream).transferTo(responseStream)
                        break
                }
                responseStream.flush()
                responseStream.close()
                if(! response.status) {
                    response.status(HttpStatus.OK_200)
                }
            }
        } else if (filterChain) {
            filterChain.doFilter(request, response)
        } else {
            Log.d("The requested path was not found: %s", request.uri())
            response.status(HttpStatus.NOT_FOUND_404)
        }
        log(request, response)
    }

    @Override
    void handle(String target, JettyRequest jettyRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Request request = Request.import(jettyRequest)
        Response response = Response.import(jettyRequest.response)
        HttpMethod method = HttpMethod.fromString(request.method.trim().toUpperCase())
        if(method == null) {
            response.sendError(HttpStatus.METHOD_NOT_ALLOWED_405)
            return
        }
        doFilter(request, response, null)
        request.setHandled(response.status > 0)
    }

    @Override
    void setServer(JettyServer server) {
        jettyServer = server
    }

    @Override
    JettyServer getServer() {
        return jettyServer
    }

    @Override
    void destroy() {
        stop()
    }

    boolean webSocket(String path, WebSocketService service) {
        //TODO
        return false
    }

    void start() {
        running = true
        jettyServer.start()
        if(multiThread) {
            jettyServer.join() //TODO: check
        }
    }
    void stop() {
        jettyServer.stop()
    }

    @Override
    boolean isRunning() {
        return this.running
    }

    @Override
    boolean isStarted() {
        return this.running
    }

    @Override
    boolean isStarting() {
        return this.running
    }

    @Override
    boolean isStopping() {
        return ! this.running
    }

    @Override
    boolean isStopped() {
        return ! this.running
    }

    @Override
    boolean isFailed() {
        return false
    }

    @Override
    boolean addEventListener(EventListener listener) {
        return false
    }

    @Override
    boolean removeEventListener(EventListener listener) {
        return false
    }
/**
     * Add a new route into the server
     * @param service
     * @param path
     * @param route
     * @return
     */
    boolean add(Service service, String path, Route route) {
        boolean duplicated = definitions.any { it.path == path || matchURI(path, service.method, service.allowType).route.present }
        if (duplicated) {
            Log.w("Warning, duplicated path [" + path + "] and method [" + service.method.toString() + "] found.")
            return false
        }
        return definitions.add(new RouteDefinition(service.method, path, service.allowType, route))
    }

    boolean add(RouteDefinition definition) {
        return definitions.add(definition)
    }

    boolean isSecure() {
        return certificate?.valid
    }

    int getTimeout() {
        return idleTimeout
    }
    /**
     * Find the route according to request
     * @param request
     * @return
     */
    protected MatchFilterResult matchURI(String path, HttpMethod method, String acceptType) {
        Map<String,String> params = [:]
        RouteDefinition match = definitions.find {
            RouteDefinition rd ->
                boolean found = false
                if(rd.method == method && (rd.acceptType == "*/*" || acceptType.tokenize(",").collect {
                    it.replaceAll(/;.*$/,"")
                }.contains(rd.acceptType))) {
                    // Match exact path
                    // Match with regex (e.g. /^path/(admin|control|manager)?$/ )
                    if (rd.path == path || (rd.path.endsWith("/?") && rd.path.replace(/\/\?$/, '') == path.replace(/\/$/, ''))) {
                        //TODO verify /?
                        found = true
                    } else {
                        // Match with path variables (e.g. /path/:var/)
                        // Match with glob (e.g. /path/*)
                        Pattern pattern = null
                        if (rd.path.contains("/:") || rd.path.contains("*")) {
                            pattern = Pattern.compile(
                                rd.path.replaceAll(/\*/, "(?<>.*)")
                                    .replaceAll("/:([^/]*)", '/(?<$1>[^/]*)')
                                , Pattern.CASE_INSENSITIVE)
                        } else if (rd.path.startsWith("~/")) { //TODO: verify
                            pattern = Pattern.compile(rd.path, Pattern.CASE_INSENSITIVE)
                        }
                        if (pattern) {
                            Matcher matcher = (path =~ pattern)
                            if (matcher.find()) {
                                found = true
                                if (matcher.hasGroup()) {
                                    Matcher groupMatcher = Pattern.compile("\\(\\?<(\\w+)>").matcher(pattern.toString())
                                    //TODO: verify named groups
                                    while (groupMatcher.find()) {
                                        String groupName = groupMatcher.group(1)
                                        if (groupName) {
                                            params[groupName] = matcher.group(groupName).toString()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return found
        }
        return new MatchFilterResult(
            Optional.ofNullable(match),
            params
        )
    }

    void log(Request request, Response response) {
        if(logFile) {
            ZonedDateTime now = SysClock.now.atZone(SysClock.clock.zone)
            String query = request.queryString
            logFile << String.format("[%s] \"%s %s %s\" %d %d \"-\" \"%s\"\n",
                now.format("dd/MMM/yyyy:HH:mm:ss Z"),
                request.method.toUpperCase(),
                request.requestURI + (query ? "?" + query : ""),
                request.protocol,
                response.status,
                response.length,
                request.userAgent
            )
        }
    }
}
