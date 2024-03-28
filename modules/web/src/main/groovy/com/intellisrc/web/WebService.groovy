package com.intellisrc.web

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.etc.Cache
import com.intellisrc.etc.JSON
import com.intellisrc.etc.Mime
import com.intellisrc.etc.YAML
import com.intellisrc.net.LocalHost
import com.intellisrc.web.protocols.HttpProtocol
import com.intellisrc.web.protocols.Protocol
import com.intellisrc.web.service.*
import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import jakarta.servlet.MultipartConfigElement
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpSession
import jakarta.servlet.http.Part
import org.apache.commons.io.IOUtils
import org.codehaus.groovy.runtime.metaclass.MissingMethodExceptionNoStack
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer

import javax.imageio.ImageIO
import javax.imageio.ImageWriter
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.intellisrc.web.protocols.Protocol.HTTP
import static com.intellisrc.web.service.Compression.NONE
import static com.intellisrc.web.service.HttpHeader.*
import static com.intellisrc.web.service.ServiceOutput.Type
import static com.intellisrc.web.service.WebError.*
import static org.eclipse.jetty.http.HttpMethod.*
import static org.eclipse.jetty.http.HttpStatus.*

@CompileStatic
/**
 * Launch HTTP Service (WebSocket can be added on top of it)
 *
 * This class offers an easy way to run web services
 * It uses mainly 2 types of Services:
 * - Serviciable     : Common services
 * - ServiciableAuth : Services to initialize a session
 *
 * Multiple instances of WebService can be executed
 * in different ports
 *
 * Options:
 * resources : Path to serve static content
 * cacheTime : time to store static content in cache in seconds (0 = disabled <default>)
 * port      : Port to be used by the WebService (default: 80)
 * threads   : Maximum Number of clients
 *
 */
class WebService extends WebServiceBase {
    static String defaultCharset = Config.any.get("web.charset", "UTF-8")
    static boolean forceFile = Config.any.get("web.upload.force", false) // Throw exception when file is expected and it is empty

    public int threads = 20
    public int minThreads = 2
    public int eTagMaxKB = 1024
    public int cacheTime = Cache.DISABLED
    public int cacheMaxSizeKB = 256
    public int cacheTotalMaxSizeMB = 0 // 0 = Unlimited
    public boolean compress = true  //Compress output when possible
    public boolean embedded = false //Turn to true if resources are inside jar
    boolean trustForwardHeaders = true
    boolean checkSNIHostname = true
    boolean sniRequired = false
    public String allowOrigin = "" // disabled by default
    public List<String> indexFiles = ["index.html", "index.htm"]
    public Protocol protocol = HTTP
    public FilePolicy filePolicy = { File file -> true }
    public PathPolicy pathPolicy = { String path -> true }
    public RequestPolicy requestPolicy = { Request request -> true }
    public WebErrorTemplate errorTemplate = defaultErrorTemplate
    public final Cache<ServiceOutput> cache = new Cache<ServiceOutput>(timeout: Cache.FOREVER)

    protected List<StaticPath> staticPaths = []
    protected Server jettyServer
    protected ServletContextHandler contextHandler
    protected RequestHandle requestHandle
    protected boolean multiThread
    protected List<Serviciable> services = []
    protected final ConcurrentLinkedQueue<Service> definitions = new ConcurrentLinkedQueue<>()

    static interface FilePolicy {
        boolean allow(File file)
    }

    static interface PathPolicy {
        boolean allow(String path)
    }

    static interface RequestPolicy {
        boolean allow(Request request)
    }

    static interface StartCallback {
        void call(WebService srv)
    }

    @TupleConstructor
    static class StaticPath {
        String path = null
        boolean embedded = false
        int expireSeconds = 0
        int cacheMaxSizeKB = 0
    }

    /**
     * Initialize Server
     * Executed only once even if we restart this service
     */
    protected void init() {
        if(!initialized) {
            initialized = true
            try {
                if(ssl && !secure) {
                    Log.w("KeyStore is invalid. Not using SSL.")
                    ssl = null
                }
                HttpProtocol httpProtocol = protocol.get(this)
                httpProtocol.checkSNIHostname = checkSNIHostname
                httpProtocol.trustForwardHeaders = trustForwardHeaders
                httpProtocol.sniRequired = sniRequired

                if(minThreads > threads) { minThreads = threads }
                this.multiThread = threads > 0
                jettyServer = multiThread ? new Server(new QueuedThreadPool(threads, minThreads, timeout)) : new Server()
                jettyServer.addConnector(httpProtocol.connector)
                requestHandle = new RequestHandle(this)
                contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS)
                HandlerList handlers = new HandlerList()
                handlers.setHandlers([requestHandle, contextHandler].toArray() as Handler[])
                jettyServer.setHandler(handlers)
                Log.i("Using protocol: %s, %s", protocol, secure ? "with SSL" : "unencrypted")
            } catch(Exception e) {
                Log.e("Unable to initialize web service", e)
            }
        }
    }
    /**
     * start and specify callback "onStart"
     * @param onStart
     * @return
     */
    WebService start(StartCallback onStart) {
        start(false, onStart)
    }
    /**
     * start web service
     * It will add all specified services into routes
     * and launch the Jetty Server
     */
    WebService start(boolean background = false, StartCallback onStart = null) {
        init()
        try {
            if (LocalHost.isPortAvailable(port, address)) {
                Log.i("Starting server in port $port with pool size of $threads")
                // Preparing a service (common between Services and SingleService):
                services.each {
                    final Serviciable serviciable ->
                        boolean prepared
                        switch (serviciable) {
                            case ServiciableMultiple:
                                ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                                prepared = serviciables.services.every {
                                    Service sp ->
                                        setupService(serviciable, sp)
                                }
                                break
                            case ServiciableSingle:
                                Service sp = (serviciable as ServiciableSingle).service
                                prepared = setupService(serviciable, sp)
                                break
                            case ServerSentEvent:
                                ServerSentEvent sse = serviciable as ServerSentEvent
                                Log.v("Adding SSE Service at path: [%s]", sse.path)

                                ServletContextHandler sseContext = new ServletContextHandler(ServletContextHandler.SESSIONS)
                                sseContext.setContextPath("/")
                                ServletHolder holder = new ServletHolder(sse.servlet)
                                holder.initOrder = 0
                                sseContext.addServlet(holder, sse.path)

                                jettyServer.handler = new HandlerList(requestHandle, sseContext, contextHandler)
                                // We set reserved services to prevent other services to use the same path:
                                prepared = setupService(serviciable, new Service(
                                    method: GET,
                                    reserved : true
                                ))
                                break
                            case ServiciableWebSocket:
                                ServiciableWebSocket websocket = serviciable as ServiciableWebSocket
                                Log.v("Adding WebSocket Service at path: [%s]", websocket.path)
                                ServletHolder holder = new ServletHolder(websocket.webSocketService)
                                holder.initOrder = 0
                                contextHandler.addServlet(holder, websocket.path)
                                JettyWebSocketServletContainerInitializer.configure(contextHandler, null)
                                // We set reserved services to prevent other services to use the same path:
                                prepared = setupService(serviciable, new Service(
                                    method: CONNECT,
                                    reserved : true
                                )) && setupService(serviciable, new Service(
                                    method: GET,
                                    reserved : true
                                ))
                                break
                            case ServiciableAuth:
                                prepared = true
                                //do nothing, skip
                                break
                            default:
                                throw new Exception("Interface not implemented: ${serviciable.class.simpleName}")
                        }
                        if (!prepared) {
                            Log.w("Failed to prepare one or more services")
                        }
                        switch (serviciable) {
                            case ServiciableAuth:
                                ServiciableAuth auth = serviciable as ServiciableAuth
                                setupService(serviciable, Service.new(POST, auth.path + auth.loginPath, {
                                    Request request, Response response ->
                                        //noinspection GroovyUnusedAssignment
                                        boolean ok = false
                                        Map<String, Object> sessionMap = auth.onLogin(request, response)
                                        Map res = [:]
                                        if (!sessionMap.isEmpty()) {
                                            ok = true
                                            HttpSession session = request.session
                                            //noinspection GroovyMissingReturnStatement
                                            sessionMap.each {
                                                if (it.key == "response" && it.value instanceof Map) {
                                                    //noinspection GrReassignedInClosureLocalVar
                                                    res += (it.value as Map)
                                                } else {
                                                    session.setAttribute(it.key, it.value)
                                                }
                                            }
                                            res.id = session.id
                                        } else {
                                            Log.w("Unauthorized: %s", request.uri())
                                            throw new WebException(UNAUTHORIZED_401)
                                        }
                                        response.type(Mime.JSON)
                                        res.ok = ok
                                        return res
                                }, auth.acceptType))
                                setupService(serviciable, Service.new(GET, auth.path + auth.logoutPath, {
                                    Request request, Response response ->
                                        boolean ok = auth.onLogout(request, response)
                                        if (ok) {
                                            request.session?.invalidate()
                                        }
                                        response.type(Mime.JSON)
                                        return [
                                            ok: ok
                                        ]
                                }, auth.acceptType))
                                break
                        }
                }
                running = true
                jettyServer.start()
                ssl = null // Removed from memory for security
                if (onStart) {
                    onStart.call(this)
                }
                if (background) {
                    //Wait until the server is Up
                    sleep(Millis.SECOND)
                } else {
                    if (multiThread) {
                        jettyServer.join() //TODO: check
                    } else {
                        while (running) {
                            sleep(Millis.SECOND)
                        }
                    }
                }
            } else {
                Log.w("Port %d is already in use", port)
            }
        } catch(WebException we) {
            // It should have been handled before here
            Log.w("Not handled: Web service error: %s", we)
        } catch(Throwable e) {
            Log.e("Unable to start WebService", e)
        }
        return this
    }

    /**
     * Specify static path and cache rules
     * @param path
     * @param expirationSec
     * @param cacheMaxSizeKB
     * @return
     */
    WebService setStaticPath(String path, int expirationSec, int cacheMaxSizeKB, boolean embedded) {
        return setStaticPath(new StaticPath(path, embedded, expirationSec, cacheMaxSizeKB))
    }
    /**
     * Add a StaticPath
     * @param path
     * @param expirationSec
     * @param cacheMaxSizeKB
     * @return
     */
    WebService setStaticPath(StaticPath path) {
        this.staticPaths << path
        return this
    }
    /**
     * Common code for ServiciableSingle and ServiciableMultiple
     * @param serviciable
     * @param sp
     */
    protected boolean setupService(Serviciable serviciable, Service sp) {
        // If Serviciable specifies allowOrigin and the Service doesn't, set it.
        if(serviciable.allowOrigin != null && sp.allowOrigin == null) {
            sp.allowOrigin = serviciable.allowOrigin
        }
        sp.acceptType = serviciable.acceptType
        if(! sp.acceptType) {
            sp.acceptType = "*/*"
        }
        sp.acceptCharset = serviciable.acceptCharset
        // Fix path:
        sp.path = addRoot(serviciable.path, sp.path)
        // Copy from Serviciable (General) to Service (Particular):
        if(! sp.allow && serviciable.allow ) {
            sp.allow = serviciable.allow
        }
        // If allow is not set in any level, set default:
        if(! sp.allow) {
            sp.allow = { true } as Service.Allow
        }
        if(! sp.beforeRequest && serviciable.beforeRequest) {
            sp.beforeRequest = serviciable.beforeRequest
        }
        if(! sp.beforeResponse && serviciable.beforeResponse) {
            sp.beforeResponse = serviciable.beforeResponse
        }
        if(! sp.onError && serviciable.onError) {
            sp.onError = serviciable.onError
        }
        Log.v("Adding Service: [%s] with method %s", sp.path, sp.method.toString())
        return addService(sp)
    }

    /**
     * Get output content type and content
     * @param service (add link)
     * @param res (response from Service.Action)
     * @param contentType
     */
    protected static ServiceOutput handleContentType(Object res, String contentType, String charSet, boolean forceBinary, Compression compress) {
        // Skip this if the object is ServiceOutput
        if(res instanceof ServiceOutput) {
            return res
        }
        ServiceOutput output = new ServiceOutput(
            contentType: contentType?.toLowerCase() ?: "",
            charSet : charSet,
            content: res,
            compression: compress
        )
        // All Collection objects convert them to List so they are cleanly converted
        if(res instanceof Collection) {
            output.content = res.toList()
        }
        if(res instanceof Number) {
            output.content = res.toString()
        }
        if(output.contentType) {
            if(output.contentType == Mime.SSE) {
                output.type = Type.STREAM
            } else {
                output.type = Type.fromString(output.contentType)
                output.fileName = "download." + output.contentType.tokenize("/").last()
            }
        } else { // Auto detect contentType:
            //noinspection GroovyFallthrough
            switch (output.content) {
                case String:
                    String resStr = output.type.toString()
                    output.type = Type.TEXT
                    switch (true) {
                        case resStr.contains("<html") :
                            output.contentType = Mime.HTML
                            output.fileName = "download.html"
                            break
                        case resStr.contains("<?xml") :
                            output.contentType = Mime.XML
                            output.fileName = "download.xml"
                            break
                        case resStr.contains("<svg") :
                            output.contentType = Mime.SVG
                            output.fileName = "download.svg"
                            break
                        default :
                            output.contentType = Mime.getType(new ByteArrayInputStream(resStr.bytes)) ?: Mime.TXT
                            if(output.contentType == Mime.TXT) {
                                output.fileName = "download.txt"
                            } else {
                                output.fileName = "download." + output.contentType.tokenize("/").last()
                            }
                    }
                    break
                case File:
                    File file = output.content as File
                    if(file.exists()) {
                        output.contentType = Mime.getType(file)
                        output.type = Type.fromString(output.contentType)
                        output.fileName = file.name
                    } else {
                        output.responseCode = NOT_FOUND_404
                    }
                    break
                case BufferedImage:
                    BufferedImage img = output.content as BufferedImage
                    boolean hasAlpha = img.colorModel.hasAlpha()
                    String ext = hasAlpha ? "png" : "jpg" //TODO: support other?
                    output.type = Type.IMAGE
                    output.contentType = Mime.getType(ext)
                    output.fileName = "download.${ext}"
                    break
                case List:
                case Map:
                    output.type = Type.JSON
                    output.contentType = Mime.JSON
                    output.fileName = "download.json"
                    break
                case URL:
                    URL url = output.content as URL
                    HttpURLConnection conn = url.openConnection() as HttpURLConnection
                    conn.setRequestMethod("GET")
                    conn.connect()
                    output.contentType = conn.contentType ?: Mime.getType(url)
                    output.content = conn.content
                    output.responseCode = conn.responseCode
                    output.type = Type.fromString(output.contentType)
                    output.fileName = url.file
                    return output // Do not proceed to prevent changing content
                    break
                default:
                    output.type = Type.BINARY
                    output.contentType = "" //Unknown type
                    output.fileName = "download.bin"
            }
        }
        if(output.responseCode && output.responseCode >= BAD_REQUEST_400) {
            output.content = null
            output.size = 0
            output.compression = NONE
        } else {
            if (forceBinary) {
                output.type = Type.BINARY
            }
            // Set Size
            switch (output.content) {
                case String:
                    output.content = output.content.toString()
                    output.size = (output.content as String).bytes.length //Support Unicode (using bytes instead of String)
                    break
                case File:
                    File file = output.content as File
                    output.content = file.bytes
                    output.size = file.size() as int
                    output.etag = file.lastModified().toString()
                    break
                case byte[]:
                    output.size = (output.content as byte[]).length
                    break
            }
            // Set encoded content
            switch (output.type) {
                case Type.TEXT:
                    if (!output.size) {
                        output.content = output.content.toString()
                        output.size = (output.content as String).bytes.length
                        //Support Unicode (using bytes instead of String)
                    }
                    break
                case Type.JSON:
                    //noinspection GroovyFallthrough
                    switch (output.content) {
                        case Collection:
                        case Map:
                            output.content = JSON.encode(output.content)
                            output.size = (output.content as String).size()
                            break
                    }
                    break
                case Type.YAML:
                    //noinspection GroovyFallthrough
                    switch (output.content) {
                        case Collection:
                        case Map:
                            output.content = YAML.encode(output.content)
                            output.size = (output.content as String).size()
                            break
                    }
                    break
                case Type.IMAGE:
                    output.charSet = ""
                    switch (output.content) {
                        case BufferedImage:
                            BufferedImage img = output.content as BufferedImage
                            ByteArrayOutputStream os = new ByteArrayOutputStream()
                            ImageWriter iw = ImageIO.getImageWritersByMIMEType(output.contentType).next()
                            if (iw) {
                                iw.setOutput(ImageIO.createImageOutputStream(os))
                                iw.write(img)
                                output.content = os.toByteArray()
                            }
                            os.close()
                            break
                    }
                    // Replace it with "Binary"
                    output.type = Type.BINARY
                    output.size = (output.content as byte[]).length
                    break
                case Type.BINARY:
                    output.charSet = ""
                    switch (output.content) {
                        case String:
                            output.content = output.content.toString().bytes
                            break
                        case BufferedImage: // Output raw bytes:
                            BufferedImage img = output.content as BufferedImage
                            output.content = ((DataBufferByte) img.raster.dataBuffer).data
                            break
                        default:
                            output.content = output.content as byte[]
                    }
                    output.size = (output.content as byte[]).length
            }
        }
        // If file type is compressed by default, do not compress
        if(output.compression != NONE && Mime.isCompressed(output.contentType)) {
            output.compression = NONE
        }
        return output
    }

    /**
     * Get current request key
     * @param request
     * @return
     */
    static String getCacheKey(Request request) {
        String query = request.queryString
        return request.uri() + (query ? "?" + query : "")
    }

    /**
     * Process Service and return ServiceOutput
     * In this step, we first set all response headers based on the Service
     * options. Some headers may be override by prepareResponse()
     *
     * @param request
     * @param response
     * @param sp
     * @return
     */
    ServiceOutput processService(Service sp, Request request, Response response) {
        ServiceOutput output
        // First we use global headers
        Map<String, String> outHeaders = globalHeaders
        // We modify headers according to response (pre-action-execution):
        outHeaders.putAll(response.headers)
        // Then, we add or override with service headers
        outHeaders.putAll(sp.headers) // Manually specified headers have higher priority than automatic generated headers
        // --------------------------------------------------------------------------------------------------------------
        List<Compression> clientSupportedEncodings = request.acceptedEncodings

        // Only Allowed clients:
        if (sp.allow.check(request)) {
            File tempDir = File.tempDir
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempDir.absolutePath))
            boolean hasParts = false
            try {
                hasParts = !request.parts.empty
            } catch(Exception ignored) {}
            // If we have uploads:
            if (hasParts) {
                if (tempDir.canWrite()) {
                    List<UploadFile> uploadFiles = []
                    if (request.contentLength > 0) {
                        request.parts.each {
                            Part part ->
                                if(part.contentType) { //Only process files with content-type (otherwise are non-file fields)
                                    if (part.size) {
                                        try {
                                            Path path = Files.createTempFile("upload", ".file")
                                            Files.copy(part.inputStream, path, StandardCopyOption.REPLACE_EXISTING)
                                            UploadFile file = new UploadFile(path.toString(), part.submittedFileName, part.name)
                                            uploadFiles << file
                                        } catch (Exception e) {
                                            handleException(sp, INTERNAL_SERVER_ERROR_500, String.format("Unable to upload file: %s", part.submittedFileName), e)
                                        }
                                    } else {
                                        if(forceFile) {
                                            handleException(sp, LENGTH_REQUIRED_411, String.format("File: %s was empty", part.submittedFileName))
                                        }
                                    }
                                }
                        }
                        try {
                            Object res = callAction(sp.action, request, response, uploadFiles, true)
                            boolean forceBinary = outHeaders.containsKey(CONTENT_TRANSFER_ENCODING) && outHeaders[CONTENT_TRANSFER_ENCODING] == "binary"
                            //noinspection GroovyUnusedAssignment : IDE mistake
                            output = handleContentType(res, sp.contentType ?: response.type(), sp.charSet, forceBinary, getCompression(clientSupportedEncodings, sp.getCompress(compress)))
                            if (output.responseCode && output.responseCode >= BAD_REQUEST_400) {
                                handleException(sp, output.responseCode, String.format("Directory is not writable: %s", tempDir.absolutePath))
                            }
                        } catch (Exception e) {
                            handleException(sp, INTERNAL_SERVER_ERROR_500, "Upload failed", e)
                        }
                        uploadFiles.each {
                            if (it.exists()) {
                                it.delete()
                            }
                        }
                    } else {
                        handleException(sp, LENGTH_REQUIRED_411, "Upload file was empty")
                    }
                } else {
                    handleException(sp, INTERNAL_SERVER_ERROR_500, "Temporally directory %s is not writable")
                }
            } else { // Normal requests: (no cache, no file upload)
                try {
                    Object res = callAction(sp.action, request, response)
                    if(res != null) {
                        boolean forceBinary = outHeaders.containsKey(CONTENT_TRANSFER_ENCODING) && outHeaders[CONTENT_TRANSFER_ENCODING] == "binary"
                        //noinspection GroovyUnusedAssignment : IDE mistake
                        output = handleContentType(res, sp.contentType ?: response.type(), sp.charSet, forceBinary,  getCompression(clientSupportedEncodings, sp.getCompress(compress)))
                        if(output.responseCode && output.responseCode >= BAD_REQUEST_400) {
                            handleException(sp, output.responseCode, "Exception in Service")
                        }
                    } else {
                        handleException(sp, NOT_FOUND_404, String.format("Not Found : %s", request.uri()))
                    }
                } catch (Exception e) {
                    handleException(sp, INTERNAL_SERVER_ERROR_500, "Service action failed", e)
                }
            }

            // ------------------- After action is processed ----------------
            if(output != null) {
                // Import global and service headers:
                output.importHeaders(outHeaders)
                //noinspection GroovyUnusedAssignment
                outHeaders = null // Do not use it anymore (we modify output.headers directly)

                // If it is not a stream, check download and etag
                if(output.type != Type.STREAM) {
                    // Set download : if "Content-Disposition" is set on headers this is not required:
                    if (sp.download || output.contentType == Mime.BIN) {
                        output.downloadName = sp.downloadFileName ?: output.fileName
                    }
                    // Set ETag: (even if we compress it later, we keep the original Etag of content)
                    String etag = output.etag = sp.etag?.calc(output.content) ?: output.etag
                    if (!etag && output.size) {
                        try {
                            if (output.type == Type.BINARY) {
                                if (output.size > 1024 * eTagMaxKB) {
                                    Log.v("Unable to generate ETag for: %s, output is Binary, you can add 'etag' property in Service or increment 'eTagMaxKB' property to dismiss this message", request.uri())
                                } else {
                                    etag = (output.content as byte[]).md5()
                                }
                            } else {
                                etag = output.content.toString().md5()
                            }
                            if (etag) {
                                output.etag = etag
                            } else {
                                Log.v("Unable to generate ETag for: %s, unknown reason", request.uri())
                            }
                        } catch (Exception e) {
                            //Can't be converted to String
                            Log.v("Unable to set ETag for: %s, failed : %s", request.uri(), e)
                        }
                    }
                    // Do not compress or return anything if we have the same etag
                    String prevTag = request.headers(IF_NONE_MATCH)
                    if (prevTag == etag) { // Same content
                        output.setNotModified()
                    }
                }
                // Compress if requested
                if(output && output.size && sp.getCompress(compress)) {
                    if(output.size > sp.minCompressBytes) {
                        response.compression = getCompression(clientSupportedEncodings, true) // Get automatically the best option
                        byte[] bytes = []
                        switch (output.content) {
                            case String:
                                bytes = output.content.toString().bytes
                                break
                            case File:
                                bytes = (output.content as File).bytes
                                break
                            case OutputStream:
                                ByteArrayOutputStream baos = new ByteArrayOutputStream()
                                baos.writeTo(output.content as OutputStream)
                                bytes = baos.toByteArray()
                                break
                            case byte[]:
                                bytes = output.content as byte[]
                                break
                            default: // Do not compress here
                                output.compression = NONE
                                break
                        }
                        if (bytes.size() > 0 && output.compression != NONE) {
                            output.content = response.compression.compress(bytes)
                            output.size = (output.content as byte[]).size()
                        }
                    } else if(output.size) {
                        output.compression = NONE
                        Log.v("Content was not compressed as it is smaller than 'minCompressBytes': %d < %d", output.size, sp.minCompressBytes)
                    }
                }
            } else {
                handleException(sp, NOT_FOUND_404, String.format("Not found: %s", request.uri()))
            }
        } else { // Unauthorized
            Log.w("Forbidden: %s", request.uri())
            throw new WebException(FORBIDDEN_403)
        }
        return output
    }
    /**
     * Prepare response headers based on ServiceOutput
     * In this step, we set headers based on the ServiceOutput (which may override
     * those set by the Service in processService)
     *
     * @param output
     * @param response
     */
    protected void prepareResponse(ServiceOutput output, Response response) {
        // Import headers from ServiceOutput:
        output.headers.each {
            response.header(it.key, it.value)
        }
        // Apply content-type:
        if(output.contentType) {
            response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
        }
        // Pass response code from output:
        if(output.responseCode) {
            response.status(output.responseCode)
        }

        // Set download : if "Content-Disposition" is set on headers this is not required:
        if (! output.downloadName.empty) {
            response.header(CONTENT_DISPOSITION, "attachment; filename=" + output.downloadName)
            if (output.type == Type.BINARY) {
                response.header(CONTENT_TRANSFER_ENCODING, "binary")
            }
        }
        // Set ETag: (even if we compress it later, we keep the original Etag of content)
        if (output.etag) {
            response.header(ETAG, output.etag)
        }

        // Set compression header
        if(output.compression != NONE) {
            response.header(CONTENT_ENCODING, output.compression.toString())
        }

        if(output.type == Type.STREAM) {
            response.header(CONNECTION, "keep-alive")
            response.header(CACHE_CONTROL, "no-cache")
            response.header(TRANSFER_ENCODING, "chunked")
        } else {
            // Set content-length
            if (output.size > 0) {
                response.header(CONTENT_LENGTH, sprintf("%d", output.size))
                // Add headers
                if (output.type == Type.BINARY) {
                    response.header(ACCEPT_RANGES, "bytes")
                }
            } else if(output.responseCode != NOT_MODIFIED_304 && output.responseCode != NOT_FOUND_404) {
                response.status(NO_CONTENT_204)
            }
        }
        if (!response.status) {
            response.status(OK_200)
        }
    }

    /**
     * Decide which compression method to use depending on client and server availability
     * @param client
     * @param server
     * @return
     */
    protected static Compression getCompression(Collection<Compression> client, boolean compress) {
        return compress ? (client.find { Compression.available.contains(it) } ?: NONE) : NONE
    }

    /**
     * This method handles the action
     * It will work with either a Closure or a Service.Action interface
     * In the case of a Closure, it will try different possibilities for the parameters
     * so it is more flexible and allow only those required.
     * Although we could allow to set Response before Request, its logically better to
     * only allow Response after Request (File can be set before or after them).
     * @param action
     * @param request
     * @param response
     * @return
     */
    protected static Object callAction(final Object action, final Request request, final Response response, final List<UploadFile> upload = null, boolean forceUpload = false) {
        Object returned = null
        if (action instanceof Service.Action) {
            returned = action.run()
        } else if (action instanceof Service.ActionRequest) {
            returned = action.run(request)
        } else if (action instanceof Service.ActionResponse) {
            returned = action.run(request, response)
        } else if (action instanceof Service.Upload) {
            returned = action.run(upload.first())
        } else if (action instanceof Service.UploadRequest) {
            returned = action.run(upload.first(), request)
        } else if (action instanceof Service.UploadResponse) {
            returned = action.run(upload.first(), request, response)
        } else if (action instanceof Service.Uploads) {
            returned = action.run(upload)
        } else if (action instanceof Service.UploadsRequest) {
            returned = action.run(upload, request)
        } else if (action instanceof Service.UploadsResponse) {
            returned = action.run(upload, request, response)
        } else if (action instanceof Closure) {
            if (upload || forceUpload) {
                switch (true) {
                    case tryCall(action, { returned = it }, upload, request, response): break
                    case tryCall(action, { returned = it }, upload.first(), request, response): break
                    case tryCall(action, { returned = it }, request, response, upload): break
                    case tryCall(action, { returned = it }, request, response, upload.first()): break
                    case tryCall(action, { returned = it }, upload, request): break
                    case tryCall(action, { returned = it }, upload.first(), request): break
                    case tryCall(action, { returned = it }, upload, response): break
                    case tryCall(action, { returned = it }, upload.first(), response): break
                    case tryCall(action, { returned = it }, request, upload): break
                    case tryCall(action, { returned = it }, request, upload.first()): break
                    case tryCall(action, { returned = it }, response, upload): break
                    case tryCall(action, { returned = it }, response, upload.first()): break
                    case tryCall(action, { returned = it }, upload): break
                    case tryCall(action, { returned = it }, upload.first()): break
                    default:
                        Log.w("Unknown parameters expected in Service.Action as Closure. Request must be before Response and at least File must be specified.")
                }
            } else {
                switch (true) {
                    case tryCall(action, { returned = it }, request, response): break
                    case tryCall(action, { returned = it }, request): break
                    case tryCall(action, { returned = it }, response): break
                    case tryCall(action, { returned = it }): break
                    default:
                        Log.w("Unknown parameters expected in Service.Action as Closure. Request must be before Response")
                }
            }
        }
        return returned
    }

    /**
     * Try to call a closure with different parameters
     * @param action
     * @param returnValue
     * @param params
     */
    protected static boolean tryCall(Closure action, Closure returnValue, Object... params) {
        boolean called = true
        int np = action.maximumNumberOfParameters
        int ap = params.size()
        try {
            //noinspection GroovyFallthrough
            switch (true) {
                case np == 3 && ap == 3:
                    returnValue(action.call(params[0], params[1], params[2]))
                    break
                case np == 2 && ap == 2:
                    returnValue(action.call(params[0], params[1]))
                    break
                case np == 1 && ap == 1:
                    returnValue(action.call(params[0]))
                    break
                case np == 0 && ap == 0:
                    returnValue(action.call())
                    break
                case np > 3:
                    Log.w("Action has more than 3 parameters")
                default:
                    called = false
            }
        } catch (MissingMethodException e) {
            if(e instanceof MissingMethodExceptionNoStack) {
                Log.e("MissingMethodException inside action. ", e)
            }
            called = false
        }
        return called
    }

    /**
     * Add a Service to the controller
     * This method is chainable
     * @param srv
     * @return
     */
    WebService add(Serviciable srv) {
        addService(srv)
        return this
    }

    /**
     * Add a single Service
     * @param srv
     * @return
     */
    WebService add(Service srv) {
        addService(new ServiciableSingle() {
            @Override
            Service getService() {
                return srv
            }
        })
        return this
    }

    /**
     * Adds Services to the controller
     * @param srv : Serviciable implementation
     */
    void addService(Serviciable srv) {
        init()
        if (!running) {
            if (srv instanceof ServiciableWebSocket) {
                services.add(0, srv)
            } else if(srv) {
                services << srv
            } else {
                Log.e("Invalid instance added as service: %s", srv)
            }
        } else {
            Log.w("WebService is already running. You can not add more services")
        }
    }

    /**
     * Add resources either by File (recommended method) or String
     * @param path
     */
    void setResources(Object path) {
        if (!running) {
            if (path instanceof File) {
                setStaticPath(path.absolutePath, cacheTime, cacheMaxSizeKB, false)
            } else if (path instanceof String) {
                setStaticPath(path, cacheTime, cacheMaxSizeKB, embedded)
            } else if (path instanceof Collection) {
                path.each {
                    setStaticPath(it.toString(), cacheTime, cacheMaxSizeKB, embedded)
                }
            } else {
                Log.w("Value passed to resources is not a File, String or List: %s", path.toString())
            }
        } else {
            Log.w("WebService is already running. You can not change the resource path")
        }
    }

    /**
     * Process the path filter. Here we decide what to serve.
     * If we match a Service, we execute its action, otherwise we
     * locate a static file or return 404
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     */
    boolean doFilter(ServletRequest servletRequest, ServletResponse servletResponse) {
        boolean commited = false
        boolean reserved = false
        Request request = servletRequest as Request
        Response response = servletResponse as Response
        ServiceOutput out
        if(! request.method || fromString(request.method.trim().toUpperCase()) == null) {
            Log.w("Method not allowed: %s", request.method)
            throw new WebException(METHOD_NOT_ALLOWED_405)
        }
        Cache.CacheAccess onStore = {
            response.header(SERVER_CACHE, cacheSize.toString())
        }
        Cache.CacheAccess onHit = {
            response.header(SERVER_CACHE, "true")
        }
        if (requestPolicy.allow(request)) {
            String cacheKey = getCacheKey(request)
            // Try first with cache:
            out = cache.get(cacheKey)
            if (out) {
                onHit.call(cacheKey)
            }

            // Lok for static files:
            if (!out) {
                // The request is already clean from Jetty and without query string:
                String uri = request.requestURI
                if (uri && !uri.empty) {
                    if(! staticPaths.empty) {
                        staticPaths.any {
                            StaticPath staticPath ->
                                (uri.endsWith("/") ? indexFiles.collect { uri + it } : [uri]).each {
                                    String uriPath ->
                                        String fullPath = staticPath.path + "/" + uriPath
                                        // Guess Mime based on path (with index)
                                        if(uri =~ /\.\w+$/) { // If has extension
                                            response.type(Mime.getType(uri))
                                        }
                                        if (staticPath.embedded) {
                                            if (pathPolicy.allow(fullPath)) {
                                                try {
                                                    InputStream inst = this.class.getResourceAsStream(fullPath)
                                                    byte[] bytes = IOUtils.toByteArray(inst)
                                                    boolean addToCache = staticPath.expireSeconds &&
                                                        (bytes.length / 1024 <= staticPath.cacheMaxSizeKB) && !cacheFull
                                                    Closure<ServiceOutput> noCache = {
                                                        processService(new Service(
                                                            compress: compress,
                                                            cacheTime: cacheTime,
                                                            maxAge: cacheTime,
                                                            allow: { true } as Service.Allow,
                                                            action: { return bytes }
                                                        ), request, response)
                                                    }
                                                    if(addToCache) {
                                                        out = cache.get(cacheKey, null, onHit)
                                                        if(out != null) {
                                                            out = noCache()
                                                            if(out.size) {
                                                                cache.set(cacheKey, out, onStore)
                                                            }
                                                        }
                                                    } else {
                                                        out = noCache()
                                                    }
                                                } catch (Exception e) {
                                                    Log.w("Unable to read resource from jar: %s (%s)", fullPath, e)
                                                    throw new WebException(NOT_FOUND_404)
                                                }
                                            } else {
                                                Log.w("Unauthorized: %s", request.uri())
                                                throw new WebException(UNAUTHORIZED_401)
                                            }
                                        } else {
                                            File staticFile = File.get(fullPath)
                                            if (filePolicy.allow(staticFile) && pathPolicy.allow(staticFile.absolutePath)) {
                                                if (staticFile.exists()) {
                                                    boolean addToCache = staticPath.expireSeconds &&
                                                        (staticFile.size() / 1024 <= staticPath.cacheMaxSizeKB) && !cacheFull

                                                    Closure<ServiceOutput> noCache = {
                                                        processService(new Service(
                                                            compress: compress,
                                                            cacheTime: cacheTime,
                                                            maxAge: cacheTime,
                                                            allow: { true } as Service.Allow,
                                                            action: { return staticFile }
                                                        ), request, response)
                                                    }
                                                    if(addToCache) {
                                                        out = cache.get(cacheKey, null, onHit)
                                                        if(out != null) {
                                                            out = noCache()
                                                            if(out.size) {
                                                                cache.set(cacheKey, out, onStore)
                                                            }
                                                        }
                                                    } else {
                                                        out = noCache()
                                                    }
                                                }
                                            } else {
                                                Log.w("Unauthorized: %s", request.uri())
                                                throw new WebException(UNAUTHORIZED_401)
                                            }
                                        }
                                }
                                return out != null
                        }
                    }
                } else { // Very unlikely that will end up here:
                    Log.w("Invalid request (empty)")
                    throw new WebException(BAD_REQUEST_400)
                }
            }

            // Then check services:
            if (!out) {
                MatchFilterResult mfr = matchURI(
                    request.requestURI,
                    fromString(request.method.trim().toUpperCase()),
                    request.headers(ACCEPT),
                    request.headers(ACCEPT_CHARSET)
                )
                if (mfr.route.present) {
                    request.setPathParameters(mfr.params)   // Inject params to request
                    Service sp = mfr.route.get()
                    // Call hook:
                    if(sp.beforeRequest) {
                        sp.beforeRequest.run(request)
                    }
                    if(sp.reserved) { // Skip reserved
                        reserved = true
                    } else {
                        if (sp.cacheTime) { // Check if its in Cache
                            boolean addToCache = sp.cacheTime && !cacheFull
                            Closure noCache = {
                                //noinspection GroovyUnusedAssignment : IDE mistake
                                ServiceOutput toSave = null
                                try {
                                    toSave = processService(sp, request, response)
                                } catch (WebException we) {
                                    throw we
                                } catch (Exception e) {
                                    handleException(sp, INTERNAL_SERVER_ERROR_500, "Cache failure", e)
                                }
                                return toSave
                            }

                            //noinspection GroovyUnusedAssignment : IDE mistake
                            out = addToCache ? cache.get(cacheKey, { noCache() }, onHit, onStore, sp.cacheTime) : noCache()
                        } else {
                            out = processService(sp, request, response)
                        }
                    }
                    //Call hook:
                    if(sp.beforeResponse) {
                        sp.beforeResponse.run(response)
                    }
                }
            }


            // If we have output:
            if (out) {
                // Set headers according to ServiceOutput
                prepareResponse(out, response)
                // If the response is not closed yet...
                if(out.type == Type.STREAM) {
                    response.update()
                    switch (out.content) {
                        case String: // Without compression
                            String text = out.content.toString()
                            response.contentLength = text.length()
                            response.writer.write(text)
                            response.writer.flush()
                            break
                        case byte[]: // With compression
                            byte[] content = (out.content as byte[])
                            response.contentLength = out.size ?: content.length
                            response.outputStream.write(content)
                            response.outputStream.flush()
                            break
                        default:
                            Log.w("SSE Stream should be String or byte[]: %s", request.uri())
                            throw new WebException(INTERNAL_SERVER_ERROR_500, "Invalid type")
                    }
                } else if (!response.committed) {
                    //noinspection GroovyFallthrough
                    switch (out.content) {
                        case String:
                            String text = out.toString()
                            if (!text.empty) {
                                response.contentLength = out.size
                                response.writer.write(text)
                                response.writer.flush()
                                response.writer.close()
                                commited = true
                            }
                            break
                        case ByteBuffer:
                        case byte[]:
                            byte[] content = out.content instanceof ByteBuffer ?
                                    (out.content as ByteBuffer).array() : (out.content as byte[])
                            if (content.length) {
                                response.contentLength = out.size ?: content.length
                                response.outputStream.write(content)
                                response.outputStream.flush()
                                response.outputStream.close()
                                commited = true
                            }
                            break
                        case InputStream:
                            (out as InputStream).transferTo(response.outputStream)
                            response.outputStream.flush()
                            response.outputStream.close()
                            commited = true
                            break
                    }
                }
            } else if(! reserved) {
                Log.v("No output found: %s", request.uri())
                throw new WebException(NOT_FOUND_404)
            }
        } else {
            Log.w("Unauthorized: %s", request.uri())
            throw new WebException(UNAUTHORIZED_401)
        }
        if(! reserved) {
            if (!response.status || response.status == NOT_FOUND_404) {
                Log.v("The requested path was not found: %s", request.uri())
                throw new WebException(NOT_FOUND_404)
            }
            if (response.status != NOT_MODIFIED_304 && !response.type()) {
                Log.w("Response without content type: %s", request.uri())
                throw new WebException(INTERNAL_SERVER_ERROR_500)
            }
            // Handle the rest of the errors:
            if (response.status >= 400) {
                Log.v("Server status code was: %d : %s", response.status, request.uri())
                throw new WebException(response.status, getCode(response.status).message)
            }
            // For streams do not close them unless instructed to do so
            if (out.type == Type.STREAM) {//FIXME: stream
                //noinspection GroovyInfiniteLoopStatement
                while (true) {
                    sleep(Millis.SECOND)
                }
                // Close the response if it is not closed
            } else if (!commited && !response.committed) {
                response.writer.close()
            }
        }
        return commited || response.committed
    }

    /**
     * Return WebService headers according to settings
     * @return
     */
    Map<String, String> getGlobalHeaders() {
        Map<String, String> global = new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
        if (allowOrigin) {
            global.put(ACCEPT_CONTROL_ALLOW_ORIGIN, allowOrigin)
        }
        return global
    }

    /**
     * Return Jetty Server (raw)
     * @return
     */
    Server getServer() {
        return jettyServer
    }
    /**
     * Clear cache using match
     */
    boolean clearCache(String match) {
        return cache.clear(match)
    }
    /**
     * Clear cache using match
     */
    boolean clearCache(Pattern match) {
        return cache.clear(match)
    }
    /**
     * Clear all cache
     */
    boolean clearCache() {
        Log.v("All cache cleared")
        return cache.clear()
    }
    /**
     * stop web service
     */
    void stop() {
        Log.i("Stopping server running at port: $port")
        if(jettyServer.started && jettyServer.running) {
            jettyServer.stop()
        }
        running = false
    }

    /**
     * Add a new route into the server
     * @param service
     * @param path
     * @param route
     * @return
     */
    boolean addService(Service service) {
        boolean duplicated = definitions.any {
            (it.path == service.path && it.method == service.method && it.acceptType == service.acceptType) ||
            matchURI(service.path, service.method, service.acceptType, service.acceptCharset).route.present }
        if (duplicated) {
            Log.w("Warning, duplicated path [%s] and method [%s] and acceptType [%s] found.",
                service.path, service.method.toString(), service.acceptType)
            return false
        }
        return definitions.add(service)
    }
    /**
     * Handle Exceptions related to a Service. If Service.onError is specified, it will be passed over,
     * otherwise will be handled here and throw a WebException (page)
     * @param sp
     * @param code
     * @param text
     * @param e
     */
    static void handleException(Service sp, int code, String text = "", Exception e = null) {
        boolean handled = false
        if(sp.onError) {
            handled = sp.onError.call(code, e ?: new Exception(text))
        }
        if(!handled) {
            switch (true) {
                case code >= BAD_REQUEST_400:
                    throw new WebException(code, text)
                    break
                default:
                    Log.v(text)
            }
        }
    }
    /**
     * Returns the full path including the root path
     * @param rootPath
     * @param servicePath
     * @return
     */
    protected static String addRoot(String rootPath, String servicePath) {
        String fullPath
        if(servicePath.startsWith("~/")) {
            fullPath = "~/" + rootPath.replaceAll(/^\//, '').replaceAll(/\/$/,'') +
                servicePath.replaceAll(/^~\//, '')
        } else {
            boolean startsWithParam = ! servicePath.empty && [':','*'].contains(servicePath[0])
            boolean rootEndsWithSlash = rootPath.endsWith("/")
            boolean serviceStartsWithSlash = servicePath.startsWith("/")
            if(rootEndsWithSlash && serviceStartsWithSlash) { // Remove slash from service
                servicePath = servicePath.replaceAll(/^\//, '')
            }
            if(startsWithParam &&! rootEndsWithSlash) { // Add Slash to root
                rootPath = "${rootPath}/"
            }
            fullPath = rootPath + servicePath
        }
        if(! fullPath.startsWith("/") &&! fullPath.startsWith("~/")) {
            fullPath = "/${fullPath}"
        }
        return fullPath
    }
    /**
     * Find the route according to request
     * @param request
     * @return
     */
    protected MatchFilterResult matchURI(String path, HttpMethod method, String acceptType, String acceptCharset) {
        Map<String,String> params = [:]
        Service match = definitions.find {
            Service srv ->
                boolean found = false
                if(srv.method == method && (srv.acceptType == "*/*" || acceptType.tokenize(",").collect {
                    it.replaceAll(/;.*$/,"")
                }.contains(srv.acceptType)) && (! srv.acceptCharset || srv.acceptCharset == acceptCharset)) {
                    // Match exact path
                    // Match with regex (e.g. /^path/(admin|control|manager)?$/ )
                    String fullPath = srv.path
                    // Append root slash if needed:
                    if(!  (fullPath.startsWith("/") || fullPath.startsWith("~"))) {
                        fullPath = "/" + fullPath
                    }
                    if (fullPath == path ||
                        (fullPath.endsWith("/?") && fullPath.replaceAll(/\/\?$/, '') == path.replaceAll(/\/$/, ''))) {
                        found = true
                    } else {
                        // Match with path variables (e.g. /path/:var/)
                        // Match with glob (e.g. /path/*)
                        Pattern pattern = null
                        if (fullPath.contains("/:") || fullPath.contains("*")) {
                            pattern = Pattern.compile(
                                fullPath.replaceAll(/\*/, "(?<splat>.*)")
                                    .replaceAll("/:([^/]*)", '/(?<$1>[^/]*)').replaceAll(/\$$/,'') + '$'
                                , Pattern.CASE_INSENSITIVE)
                        } else if (fullPath.startsWith("~/")) {
                            pattern = Service.toPattern(fullPath)
                        }
                        if (pattern) {
                            // Strict regex should also match the starting '/', otherwise we remove it from the request:
                            String toMatch = srv.strictPath ? path : (pattern.toString().startsWith("/") ? path : path.replaceFirst(/^\//,''))
                            Matcher matcher = (toMatch =~ pattern)
                            if (matcher.find()) {
                                found = true
                                if (matcher.hasGroup()) {
                                    Matcher groupMatcher = Pattern.compile("\\(\\?<(\\w+)>").matcher(pattern.toString())
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

    /**
     * Calculate size of cache
     * @return
     */
    long getCacheSize() {
        return cache.values().sum {
            it.size
        } as Long
    }
    /**
     * True when cache is above limits
     * @return
     */
    boolean isCacheFull() {
        return cacheTotalMaxSizeMB > 0 && cacheSize > cacheTotalMaxSizeMB * 1024
    }

    @Override
    boolean isRunning() {
        return super.running && jettyServer.running
    }

    @Override
    boolean isStarted() {
        return super.running && jettyServer.started
    }

    @Override
    boolean isStarting() {
        return super.initialized && jettyServer.starting
    }

    @Override
    boolean isStopping() {
        return ! super.running && jettyServer.stopping
    }

    @Override
    boolean isStopped() {
        return ! super.running &&! jettyServer.running
    }

    @Override
    boolean isFailed() {
        return (initialized &&! super.running) || jettyServer.failed
    }
}
