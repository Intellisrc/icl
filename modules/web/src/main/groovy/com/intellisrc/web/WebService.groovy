package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.etc.Cache
import com.intellisrc.etc.JSON
import com.intellisrc.etc.Mime
import com.intellisrc.etc.YAML
import com.intellisrc.net.LocalHost
import com.intellisrc.web.protocols.Protocol
import com.intellisrc.web.service.*
import groovy.transform.CompileStatic
import jakarta.servlet.FilterChain
import jakarta.servlet.MultipartConfigElement
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool

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

import static com.intellisrc.web.service.Response.Compression.AUTO
import static com.intellisrc.web.service.Response.Compression.NONE
import static org.eclipse.jetty.http.HttpMethod.GET
import static org.eclipse.jetty.http.HttpMethod.POST

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
 * cacheTime : time to store static content in cache (0 = disabled <default>)
 * port      : Port to be used by the WebService (default: 80)
 * threads   : Maximum Number of clients
 *
 */
class WebService extends WebServiceBase implements Handler {
    static final String ACCEPT_TYPE_REQUEST_MIME_HEADER = "Accept"
    static final String CONTENT_TYPE_HEADER = "Content-Length"

    public int threads = 20
    public int minThreads = 2
    public int eTagMaxKB = 1024
    public int cacheTime = 0
    public int cacheMaxSizeKB = 256
    public boolean embedded = false //Turn to true if resources are inside jar
    protected String resources = ""
    public String allowOrigin = "" //apply by default to all
    public List<String> indexFiles = ["index.html", "index.htm"]
    public Protocol protocol = Protocol.HTTP
    public FilePolicy filePolicy = { File file -> true }
    public RequestPolicy requestPolicy = { Request request -> true }

    protected String staticPath = ""
    protected Server jettyServer
    protected boolean multiThread
    protected List<Serviciable> services = []

    protected final Cache<ServiceOutput> cache = new Cache<ServiceOutput>(timeout: Cache.FOREVER)
    protected final Cache<byte[]> staticCache = new Cache<>(timeout: Cache.FOREVER)
    protected final ConcurrentLinkedQueue<RouteDefinition> definitions = new ConcurrentLinkedQueue<>()

    static interface FilePolicy {
        boolean allow(File file)
    }

    static interface RequestPolicy {
        boolean allow(Request request)
    }
    static interface StartCallback {
        void call(WebService srv)
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
                if(minThreads > threads) { minThreads = threads }
                this.multiThread = threads > 0
                jettyServer = multiThread ? new Server(new QueuedThreadPool(threads, minThreads, timeout)) : new Server()
                jettyServer.addConnector(protocol.get(this).connector)
                jettyServer.setHandler(this)
                indexFiles.addAll(indexFiles)
                if(resources) {
                    if (!resources.isEmpty()) {
                        if (embedded) {
                            setStaticPath(resources, cacheTime, cacheMaxSizeKB)
                        } else {
                            File resFile = File.get(resources)
                            setStaticPath(resFile.absolutePath, cacheTime, cacheMaxSizeKB)
                        }
                    }
                    Log.i("Serving static resources from: %s", resources)
                }
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
    void start(StartCallback onStart) {
        start(false, onStart)
    }
    /**
     * start web service
     * this method is chainable
     */
    void start(boolean background = false, StartCallback onStart = null) {
        init()
        try {
            if (LocalHost.isPortAvailable(port)) { //FIXME: should consider bind address
                Log.i("Starting server in port $port with pool size of $threads")
                // Preparing a service (common between Services and SingleService):
                services.each {
                    final Serviciable serviciable ->
                        boolean prepared = false
                        switch (serviciable) {
                            case ServiciableMultiple:
                                ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                                prepared = serviciables.services.every {
                                    Service sp ->
                                        setupService(serviciable, sp)
                                }
                                break
                            case ServiciableSingle:
                                prepared = setupService(serviciable, (serviciable as ServiciableSingle).service)
                                break
                            case ServiciableWebSocket:
                                addWebSocketService(serviciable as ServiciableWebSocket)
                                prepared = true
                                break
                            case ServiciableAuth:
                                prepared = true
                                //do nothing, skip
                                break
                            default:
                                Log.e("Interface not implemented")
                        }
                        if(! prepared) {
                            Log.w("Failed to prepare one or more services")
                        }
                        switch (serviciable) {
                            case ServiciableAuth:
                                ServiciableAuth auth = serviciable as ServiciableAuth
                                add(new RouteDefinition(POST, auth.path + auth.loginPath, auth.allowType,{
                                    Request request, Response response ->
                                        boolean ok = false
                                        Map<String, Object> sessionMap = auth.onLogin(request, response)
                                        Map res = [:]
                                        if (!sessionMap.isEmpty()) {
                                            ok = true
                                            //FIXME: Session session = srv.createSession(request.sessionHandler.getSession(request.session.id))
                                            sessionMap.each {
                                                if(it.key == "response" && it.value instanceof Map) {
                                                    //noinspection GrReassignedInClosureLocalVar
                                                    res += (it.value as Map)
                                                } else {
                                                    //FIXME session.attribute(it.key, it.value)
                                                }
                                            }
                                            //FIXME res.id = session.id
                                        } else {
                                            response.status(HttpStatus.UNAUTHORIZED_401)
                                        }
                                        response.type("application/json")
                                        res.ok = ok
                                        return JSON.encode(res)
                                }))
                                add(new RouteDefinition(GET, auth.path + auth.logoutPath, auth.allowType,{
                                    Request request, Response response ->
                                        boolean  ok = auth.onLogout(request, response)
                                        if(ok) {
                                            request.session?.invalidate()
                                        }
                                        response.type("application/json")
                                        return JSON.encode(
                                                ok : ok
                                        )
                                }))
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
                    sleep (Millis.SECOND)
                } else {
                    if(multiThread) {
                        jettyServer.join() //TODO: check
                    } else {
                        while (running) {
                            sleep (Millis.SECOND)
                        }
                    }
                }
            } else {
                Log.w("Port %d is already in use", port)
            }
        } catch(Throwable e) {
            Log.e("Unable to start WebService", e)
        }
    }

    /**
     * Specify static path and cache rules
     * @param path
     * @param expirationSec
     * @param cacheMaxSizeKB
     * @return
     */
    WebService setStaticPath(String path, int expirationSec, int cacheMaxSizeKB) {
        this.staticPath = path
        this.cacheMaxSizeKB = cacheMaxSizeKB
        if(expirationSec) {
            staticCache.timeout = expirationSec
        }
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
        sp.allowType = serviciable.allowType
        return addServicePath(sp, serviciable.path)
    }

    /**
     * Sets WebSocketService and copy properties from this class
     * @param webSocket
     */
    WebService addWebSocketService(ServiciableWebSocket webSocket) {
        WebSocketService wss = new WebSocketService(webSocket)
        webSocket.service = wss // Mutual Link
        wss.port = port
        wss.address = address
        wss.timeout = timeout
        wss.initialized = true
        wss.ssl = ssl
        wss.log = log
        wss.accessLog = accessLog
        return this
    }

    /**
     * Adds actions into the web service and returns
     * the corresponding paths
     * @param service
     * @param rootPath
     * @return
     */
    protected boolean addServicePath(Service service, String rootPath) {
        // Remove double slashes
        String fullPath = (rootPath + service.path).replaceAll(/\/\//,"/")
        return addAction(fullPath, service)
    }

    /**
     * Adds some action to the Server
     * @param fullPath : path of service
     * @param sp : Service object
     * @param srv : Spark.Service instance
     */
    protected boolean addAction(final String fullPath, final Service sp) {
        return add(sp, fullPath, onAction(sp))
    }
    /**
     * Get output content type and content
     * @param res (response from Service.Action)
     * @param contentType
     */
    protected static ServiceOutput handleContentType(Object res, String contentType, String charSet = "UTF-8", boolean forceBinary = false) {
        // Skip this if the object is ServiceOutput
        if(res instanceof ServiceOutput) {
            return res
        }
        ServiceOutput output = new ServiceOutput(contentType: contentType.toLowerCase(), charSet : charSet, content: res)
        // All Collection objects convert them to List so they are cleanly converted
        if(res instanceof Collection) {
            output.content = res.toList()
        }
        if(res instanceof Number) {
            output.content = res.toString()
        }
        if(output.contentType) {
            output.type = ServiceOutput.Type.fromString(output.contentType)
            output.fileName = "download." + output.contentType.tokenize("/").last()
        } else { // Auto detect contentType:
            //noinspection GroovyFallthrough
            switch (output.content) {
                case String:
                    String resStr = output.type.toString()
                    output.type = ServiceOutput.Type.TEXT
                    switch (true) {
                        case resStr.contains("<html") :
                            output.contentType = Mime.getType("html")
                            output.fileName = "download.html"
                            break
                        case resStr.contains("<?xml") :
                            output.contentType = Mime.getType("xml")
                            output.fileName = "download.xml"
                            break
                        case resStr.contains("<svg") :
                            output.contentType = Mime.getType("svg")
                            output.fileName = "download.svg"
                            break
                        default :
                            output.contentType = Mime.getType(new ByteArrayInputStream(resStr.bytes)) ?: "text/plain"
                            if(output.contentType == "text/plain") {
                                output.fileName = "download.txt"
                            } else {
                                output.fileName = "download." + output.contentType.tokenize("/").last()
                            }
                    }
                    break
                case File:
                    File file = output.content as File
                    output.contentType = Mime.getType(file)
                    output.type = ServiceOutput.Type.fromString(output.contentType)
                    output.fileName = file.name
                    break
                case BufferedImage:
                    BufferedImage img = output.content as BufferedImage
                    boolean hasAlpha = img.colorModel.hasAlpha()
                    String ext = hasAlpha ? "png" : "jpg"
                    output.type = ServiceOutput.Type.IMAGE
                    output.contentType = Mime.getType(ext)
                    output.fileName = "download.${ext}"
                    break
                case List:
                case Map:
                    output.type = ServiceOutput.Type.JSON
                    output.contentType = Mime.getType("json")
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
                    output.type = ServiceOutput.Type.fromString(output.contentType)
                    output.fileName = url.file
                    return output // Do not proceed to prevent changing content
                    break
                default:
                    output.type = ServiceOutput.Type.BINARY
                    output.contentType = "" //Unknown type
                    output.fileName = "download.bin"
            }
        }
        if(forceBinary) {
            output.type = ServiceOutput.Type.BINARY
        }
        // Set content
        switch (output.type) {
            case ServiceOutput.Type.TEXT:
                switch (output.content) {
                    case File :
                        File file = output.content as File
                        output.content = file.bytes
                        output.size = file.size()
                        output.etag = file.lastModified().toString()
                        break
                    case byte[]:
                        output.size = (output.content as byte[]).length
                        break
                    default:
                        output.content = output.content.toString()
                        output.size = (output.content as String).bytes.length //Support Unicode (using bytes instead of String)
                }
                break
            case ServiceOutput.Type.JSON:
                //noinspection GroovyFallthrough
                switch (output.content) {
                    case Collection:
                    case Map:
                        output.content = JSON.encode(output.content)
                        output.size = (output.content as String).size()
                        break
                }
                break
            case ServiceOutput.Type.YAML:
                //noinspection GroovyFallthrough
                switch (output.content) {
                    case Collection:
                    case Map:
                        output.content = YAML.encode(output.content)
                        output.size = (output.content as String).size()
                        break
                }
                break
            case ServiceOutput.Type.IMAGE:
                output.charSet = ""
                switch (output.content) {
                    case File:
                        output.content = (output.content as File).bytes
                        break
                    case BufferedImage:
                        BufferedImage img = output.content as BufferedImage
                        ByteArrayOutputStream os = new ByteArrayOutputStream()
                        ImageWriter iw = ImageIO.getImageWritersByMIMEType(output.contentType).next()
                        if(iw) {
                            iw.setOutput(ImageIO.createImageOutputStream(os))
                            iw.write(img)
                            output.content = os.toByteArray()
                        }
                        os.close()
                        break
                }
                // Replace it with "Binary"
                output.type = ServiceOutput.Type.BINARY
                output.size = (output.content as byte[]).length
                break
            case ServiceOutput.Type.BINARY:
                output.charSet = ""
                switch (output.content) {
                    case String:
                        output.content = output.content.toString().bytes
                        break
                    case File:
                        output.content = (output.content as File).bytes
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
        return output
    }

    /**
     * Get current request key
     * @param request
     * @return
     */
    protected static String getCacheKey(Request request) {
        String query = request.queryString
        return request.uri() + (query ? "?" + query : "")
    }
    /**
     * Returns the Route to be added into Spark.Service based in Service object
     * @param sp : Service object
     * @return
     */
    protected Route onAction(final Service sp) {
        return {
            Request request, Response response ->
                try {
                    Log.v("Requested: %s By: %s", URLDecoder.decode(request.requestURI, "UTF-8"), request.ip())
                    ServiceOutput output

                    // Apply headers (initial): -----------------------
                    sp.headers.each {
                        String key, String val ->
                            response.header(key, val)
                    }
                    // Apply general allow origin rule:
                    if (allowOrigin) {
                        response.header("Access-Control-Allow-Origin", allowOrigin)
                    }
                    if (sp.allowOrigin) {
                        response.header("Access-Control-Allow-Origin", sp.allowOrigin)
                    }
                    if (sp.noStore) { //Never store in client
                        response.header("Cache-Control", "no-store")
                    } else if (!sp.cacheTime && !sp.maxAge) { //Revalidate each time
                        response.header("Cache-Control", "no-cache")
                    } else {
                        String priv = (sp.isPrivate) ? "private," : "" //User-specific data
                        response.header("Cache-Control", priv + "max-age=" + sp.maxAge)
                    }
                    // ------------------------------------------------

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
                                                        InputStream input = part.getInputStream()
                                                        Path path = Files.createTempFile("upload", ".file")
                                                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                                                        UploadFile file = new UploadFile(path.toString(), part.submittedFileName, part.name)
                                                        uploadFiles << file
                                                    } catch (Exception e) {
                                                        response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                                                        Log.e("Unable to upload file: %s", part.submittedFileName, e)
                                                    }
                                                } else {
                                                    response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                                                    Log.w("File: %s was empty", part.submittedFileName)
                                                }
                                            }
                                    }
                                    try {
                                        Object res = callAction(sp.action, request, response, uploadFiles)
                                        //noinspection GroovyUnusedAssignment : IDE mistake
                                        output = handleContentType(res, response.type() ?: sp.contentType, sp.charSet,
                                                response.getHeader("Content-Transfer-Encoding")?.toLowerCase() == "binary")
                                    } catch (Exception e) {
                                        response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                                        Log.e("Service.upload closure failed", e)
                                    }
                                    uploadFiles.each {
                                        if (it.exists()) {
                                            it.delete()
                                        }
                                    }
                                } else {
                                    response.status(HttpStatus.LENGTH_REQUIRED_411)
                                    Log.e("Uploaded file is empty")
                                }
                            } else {
                                response.status(HttpStatus.SERVICE_UNAVAILABLE_503)
                                Log.e("Temporally directory %s is not writable", tempDir)
                            }
                        } else if (sp.cacheTime) { // Check if its in Cache
                            //noinspection GroovyUnusedAssignment : IDE mistake
                            output = cache.get(getCacheKey(request), {
                                ServiceOutput toSave = null
                                try {
                                    Object res = callAction(sp.action, request, response)
                                    toSave = handleContentType(res,  response.type() ?: sp.contentType, sp.charSet,
                                            response.getHeader("Content-Transfer-Encoding")?.toLowerCase() == "binary")
                                } catch (Exception e) {
                                    response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                                    Log.e("Service.action CACHE closure failed", e)
                                }
                                return toSave
                            }, sp.cacheTime)
                        } else { // Normal requests: (no cache, no file upload)
                            try {
                                Object res = callAction(sp.action, request, response)
                                if(res != null) {
                                    //noinspection GroovyUnusedAssignment : IDE mistake
                                    output = handleContentType(res, response.type() ?: sp.contentType, sp.charSet,
                                        response.getHeader("Content-Transfer-Encoding")?.toLowerCase() == "binary")
                                } else {
                                    response.status(HttpStatus.NOT_FOUND_404)
                                    output = new ServiceOutput(contentType: Mime.TXT, type : ServiceOutput.Type.TEXT)
                                    response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                                    output.content = "Not Found"
                                    if(sp.download) {
                                        sp.download = false
                                    }
                                }
                            } catch (Exception e) {
                                response.status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                                Log.e("Service.action closure failed", e)
                            }
                        }

                        // ------------------- After content is processed ----------------
                        if(output) {
                            // Apply content-type:
                            if(output.contentType) {
                                response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                            }
                            // Pass response code from output:
                            if(output.responseCode) {
                                response.status(output.responseCode)
                            }

                            // Set download : if "Content-Disposition" is set on headers this is not required:
                            if (sp.download || output.contentType == "application/octet-stream") {
                                String fileName = sp.downloadFileName ?: output.fileName
                                response.header("Content-Disposition", "attachment; filename=" + fileName)
                                if (output.type == ServiceOutput.Type.BINARY) {
                                    response.header("Content-Transfer-Encoding", "binary")
                                }
                            }
                            // Set ETag: (even if we compress it later, we keep the original Etag of content)
                            if (sp.etag != null) {
                                String etag = sp.etag.calc(output.content) ?: output.etag
                                if (etag) {
                                    output.etag = etag
                                    response.header("ETag", etag)
                                    String prevTag = request.headers("If-None-Match")
                                    if(prevTag == output.etag) { // Same content
                                        response.status(HttpStatus.NOT_MODIFIED_304)
                                        output = new ServiceOutput(contentType: Mime.TXT, type : ServiceOutput.Type.TEXT)
                                        response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                                        output.content = ""
                                    }
                                } else {
                                    try {
                                        if (output.type == ServiceOutput.Type.BINARY) {
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
                                            response.header("ETag", etag)
                                            String prevTag = request.headers("If-None-Match")
                                            if(prevTag == output.etag) { // Same content
                                                response.status(HttpStatus.NOT_MODIFIED_304)
                                                output = new ServiceOutput(contentType: Mime.TXT, type : ServiceOutput.Type.TEXT)
                                                response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                                                output.content = ""
                                            }
                                        } else {
                                            Log.v("Unable to generate ETag for: %s, unknown reason", request.uri())
                                        }
                                    } catch (Exception e) {
                                        //Can't be converted to String
                                        Log.v("Unable to set ETag for: %s, failed : %s", request.uri(), e.message)
                                    }
                                }
                            }
                            // Compress if requested
                            if(sp.compress) {
                                response.compression = AUTO
                                if(sp.compressSize) { //Unless we specify to calculate size, we do it here:
                                    response.compression = AUTO.get() // Get automatically the best option
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
                                            sp.compressSize = false
                                            response.compression = NONE
                                            break
                                    }
                                    if(bytes.size() > 0) {
                                        output.content = response.compression.compress(bytes, response)
                                        output.size = (output.content as byte[]).size()
                                    }
                                }
                            }
                            // Set content-length
                            if(output.size > 0 && response.compression != AUTO) {
                                response.header("Content-Length", sprintf("%d", output.size))
                            }
                            // Add headers
                            if (output.type == ServiceOutput.Type.BINARY) {
                                response.header("Accept-Ranges", "bytes")
                            }
                        } else {
                            response.status(HttpStatus.NOT_FOUND_404)
                            output = new ServiceOutput(contentType: Mime.TXT, type : ServiceOutput.Type.TEXT)
                            response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                            output.content = "Not Found"
                        }
                    } else { // Unauthorized
                        response.status(HttpStatus.FORBIDDEN_403)
                        if(output == null) {
                            output = new ServiceOutput(contentType: sp.contentType, charSet: sp.charSet, type: ServiceOutput.Type.fromString(sp.contentType))
                            response.type(output.contentType + (output.charSet ? "; charset=" + output.charSet : ""))
                        }
                        switch (output.type) {
                            case ServiceOutput.Type.JSON:
                                output.content = JSON.encode(ok : false, error : HttpStatus.FORBIDDEN_403)
                                break
                            case ServiceOutput.Type.YAML:
                                output.content = YAML.encode(ok : false, error : HttpStatus.FORBIDDEN_403)
                                break
                            default:
                                response.type(Mime.getType("txt") + (output.charSet ? "; charset=" + output.charSet : ""))
                                output.content = "Unauthorized"
                        }
                    }
                    return output.content
                } catch(Throwable e) {
                    Log.e("Unexpected Exception", e)
                }
        } as Route
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
    protected static Object callAction(final Object action, final Request request, final Response response, final List<UploadFile> upload = null) {
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
            if (upload) {
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
        boolean called = false
        try {
            switch (params.size()) {
                case 3:
                    returnValue(action.call(params[0], params[1], params[2]))
                    break
                case 2:
                    returnValue(action.call(params[0], params[1]))
                    break
                case 1:
                    returnValue(action.call(params[0]))
                    break
                case 0:
                    returnValue(action.call())
            }
            called = true
        } catch (MissingMethodException ignore) {
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
                resources = path.absolutePath
            } else if (path instanceof String) {
                resources = path
            } else {
                Log.w("Value passed to resources is not a File or String: %s", path.toString())
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
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        Request request = servletRequest as Request
        Response response = servletResponse as Response
        Object out = null
        if(requestPolicy.allow(request)) {
            MatchFilterResult mfr = matchURI(request.requestURI, HttpMethod.fromString(request.method.trim().toUpperCase()), request.headers(ACCEPT_TYPE_REQUEST_MIME_HEADER))
            if (mfr.route.present) {
                request.setPathParameters(mfr.params)   // Inject params to request
                out = mfr.route.get().action.call(request, response)
            } else {
                // The request is already clean from Jetty and without query string:
                String uri = request.requestURI
                if (uri && !uri.empty) {
                    List<String> options = uri.endsWith("/") ?
                        indexFiles.collect { uri + it } : [uri]
                    options.any {
                        File staticFile = File.get(staticPath, it)
                        if(filePolicy.allow(staticFile)) {
                            //noinspection GrReassignedInClosureLocalVar
                            out = staticCache.get(uri, {
                                staticFile.exists() && (staticFile.size() / 1024 <= cacheMaxSizeKB) ? staticFile.bytes : null
                            })
                            if (out == null && staticFile.exists()) {
                                //noinspection GrReassignedInClosureLocalVar
                                out = staticFile.bytes
                            }
                        } else {
                            response.status(HttpStatus.UNAUTHORIZED_401)
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
                            if (!response.getHeader(CONTENT_TYPE_HEADER)) {
                                response.header(CONTENT_TYPE_HEADER, sprintf("%d", text.length()))
                            }
                            responseStream.write(text.getBytes(response.contentType)) //TODO: test
                            break
                        case ByteBuffer:
                            out = (out as ByteBuffer).array()
                        case byte[]:
                            if (!response.getHeader(CONTENT_TYPE_HEADER)) {
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
                    if (!response.status) {
                        response.status(HttpStatus.OK_200)
                    }
                }
            } else if (filterChain) {
                filterChain.doFilter(request, response)
            } else {
                Log.d("The requested path was not found: %s", request.uri())
                response.status(HttpStatus.NOT_FOUND_404)
            }
        } else {
            response.status(HttpStatus.UNAUTHORIZED_401)
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
    void setServer(Server server) {
        jettyServer = server
    }

    @Override
    Server getServer() {
        return jettyServer
    }

    @Override
    void destroy() {
        stop()
    }

    /**
     * stop web service
     */
    void stop() {
        Log.i("Stopping server running at port: $port")
        jettyServer.stop()
        running = false
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
}
