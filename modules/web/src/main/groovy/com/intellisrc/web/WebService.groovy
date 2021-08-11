package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.etc.Cache
import com.intellisrc.etc.Mime
import groovy.transform.CompileStatic
import org.apache.tools.ant.types.resources.StringResource
import org.yaml.snakeyaml.Yaml

import javax.imageio.ImageIO
import javax.imageio.ImageWriter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

import static com.intellisrc.web.Service.Method.*

import spark.Request
import spark.Response
import spark.Route
import spark.Service as Srv

import javax.servlet.MultipartConfigElement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@CompileStatic
/**
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
class WebService {
    protected final Srv srv
    protected List<Serviciable> listServices = []
    protected List<String> listPaths = [] //mainly used to prevent collisions
    protected boolean running = false
    protected String resources = ""
    protected Cache<Output> cache = new Cache<Output>()
    // Options:
    public int cacheTime = 0
    public int port = 80
    public int threads = 20
    public int eTagMaxKB = 1024
    public boolean embedded = false //Turn to true if resources are inside jar
    public String allowOrigin = "" //apply by default to all

    static interface StartCallback {
        void call(Srv srv)
    }
    /**
     * Output types used in getOutput
     */
    protected static enum OutputType {
        JSON, YAML, TEXT, IMAGE, BINARY
    }
    protected static class Output {
        OutputType type     = OutputType.BINARY
        Object content      = null
        String contentType  = ""

        // Used by URL
        int responseCode    = 0
        // Name used to download
        String fileName     = ""
        // Size of content
        long size           = 0
        // Store eTag in some cases
        String etag         = ""
    }
    /**
     * Constructor: initializes Service instance
     */
    WebService() {
        srv = Srv.ignite()
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
     * this method is chainable
     */
    WebService start(boolean background = false, StartCallback onStart = null) {
        try {
            srv.staticFiles.expireTime(cacheTime)
            if (!resources.isEmpty()) {
                if (embedded) {
                    srv.staticFiles.location(resources)
                } else {
                    File resFile = SysInfo.getFile(resources)
                    srv.staticFiles.externalLocation(resFile.absolutePath)
                }
            }
            if (NetworkInterface.isPortAvailable(port)) {
                srv.port(port).threadPool(threads) //Initialize it right away
                Log.i("Starting server in port $port with pool size of $threads")
                listServices.each {
                    final Serviciable serviciable ->
                        switch (serviciable) {
                            case ServiciableMultiple:
                                ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                                serviciables.services.each {
                                    Service sp ->
                                        // If Serviciable specifies allowOrigin and the Service doesn't, set it.
                                        if(serviciable.allowOrigin != null && sp.allowOrigin == null) {
                                            sp.allowOrigin = serviciable.allowOrigin
                                        }
                                        if (serviciable instanceof ServiciableWebSocket) {
                                            addWebSocketService(serviciable, (serviciables.path + '/' + sp.path).replaceAll(/\/(\/+)?/, '/'))
                                        } else if (serviciable instanceof ServiciableHTTPS) {
                                            addSSLService(serviciable)
                                            addServicePath(sp, serviciables.path)
                                        } else {
                                            addServicePath(sp, serviciables.path)
                                        }
                                }
                                break
                            case ServiciableSingle:
                                ServiciableSingle single = serviciable as ServiciableSingle
                                if (serviciable instanceof ServiciableWebSocket) {
                                    addWebSocketService(single, single.path)
                                } else if (serviciable instanceof ServiciableHTTPS) {
                                    addSSLService(serviciable)
                                    addServicePath(single.service, single.path)
                                } else {
                                    addServicePath(single.service, single.path)
                                }
                                break
                            case ServiciableWebSocket:
                                addWebSocketService(serviciable, serviciable.path)
                                break
                            case ServiciableAuth:
                                //do nothing, skip
                                //TODO: do it for Multiple
                                /*ServiciableSingle single = serviciable as ServiciableSingle
                        srv.before("somePath", new SecurityFilter(single.service.config.build(), "Login"))*/
                                break
                            default:
                                Log.e("Interface not implemented")
                        }
                        switch (serviciable) {
                            case ServiciableAuth:
                                ServiciableAuth auth = serviciable as ServiciableAuth
                                srv.post(auth.path + auth.loginPath, {
                                    Request request, Response response ->
                                        boolean ok = false
                                        Map<String, Object> sessionMap = auth.onLogin(request, response)
                                        Map res = [:]
                                        if (!sessionMap.isEmpty()) {
                                            ok = true
                                            request.session(true)
                                            sessionMap.each {
                                                if(it.key == "response" && it.value instanceof Map) {
                                                    res += (it.value as Map)
                                                } else {
                                                    request.session().attribute(it.key, it.value)
                                                }
                                            }
                                            res.id = request.session().id()
                                        }
                                        response.type("application/json")
                                        res.ok = ok
                                        return JSON.encode(res)
                                })
                                srv.get(auth.path + auth.logoutPath, {
                                    Request request, Response response ->
                                        boolean  ok = auth.onLogout(request, response)
                                        if(ok) {
                                            request.session()?.invalidate()
                                        }
                                        response.type("application/json")
                                        return JSON.encode(
                                                ok : ok
                                        )
                                })
                                break
                        }
                }
                srv.init()
                running = true
                if (onStart) {
                    onStart.call(srv)
                }
                if (!background) {
                    while (running) {
                        sleep 1000L
                    }
                }
                //Wait until the server is Up
                sleep 1000L
            } else {
                Log.w("Port %d is already in use", port)
            }
        } catch(Throwable e) {
            Log.e("Unable to start WebService", e)
        }
        return this
    }

    /**
     * Sets WebSocketService
     * @param serviciable
     * @param path
     */
    protected void addWebSocketService(Serviciable serviciable, String path) {
        ServiciableWebSocket webSocket = serviciable as ServiciableWebSocket
        srv.webSocket(path, new WebSocketService(webSocket))
    }

    /**
     * Add SSL to connection
     * @param serviciable
     */
    protected void addSSLService(Serviciable serviciable) {
        ServiciableHTTPS ssl = serviciable as ServiciableHTTPS
        srv.secure(ssl.getKeyStoreFile(), ssl.getPassword(), null, null)
    }

    /**
     * Adds actions into the web service and returns
     * the corresponding paths
     * @param service
     * @param rootPath
     * @return
     */
    protected void addServicePath(Service service, String rootPath) {
        String fullPath = (rootPath + service.path).replaceAll(/\/\//,"/") //Remove double "/"
        if (listPaths.contains(service.method.toString() + fullPath)) {
            Log.w("Warning, duplicated path [" + fullPath + "] and method [" + service.method.toString() + "] found.")
        } else {
            listPaths << service.method.toString() + fullPath
            addAction(fullPath, service)
        }
    }

    /**
     * stop web service
     */
    void stop() {
        Log.i("Stopping server running at port: $port")
        srv.stop()
        running = false
    }

    /**
     * Check if server is running (in daemon mode)
     * @return
     */
    boolean isRunning() {
        return running
    }

    /**
     * Adds some action to the Spark.Service
     * @param fullPath : path of service
     * @param sp : Service object
     * @param srv : Spark.Service instance
     */
    protected void addAction(final String fullPath, final Service sp) {
        //srv."$method"(fullPath, onAction(sp)) //Dynamic method invocation: will call srv.get, srv.post, etc (not supported with CompileStatic
        switch (sp.method) {
            case GET: srv.get(fullPath, onAction(sp)); break
            case POST: srv.post(fullPath, onAction(sp)); break
            case PUT: srv.put(fullPath, onAction(sp)); break
            case DELETE: srv.delete(fullPath, onAction(sp)); break
            case OPTIONS: srv.options(fullPath, onAction(sp)); break
        }
    }
    /**
     * From content type, get OutputType
     * @param contentType
     * @return
     */
    protected static OutputType getTypeFromContentTypeString(String contentType) {
        OutputType type
        switch (contentType) {
            case ~/.*json.*/  : type = OutputType.JSON; break
            case ~/.*yaml.*/  : type = OutputType.YAML; break
            case ~/.*text.*/  : type = OutputType.TEXT; break
            case ~/.*image.*/ : type = OutputType.IMAGE; break
            default:
                type = OutputType.BINARY
        }
        return type
    }
    /**
     * Get output content type and content
     * @param res (response from Service.Action)
     * @param contentType
     */
    protected static Output handleContentType(Object res, String contentType) {
        Output output = new Output(contentType: contentType.toLowerCase(), content: res)
        // All Collection objects convert them to List so they are cleanly converted
        if(res instanceof Collection) {
            output.content = res.toList()
        }
        if(res instanceof Number) {
            output.content = res.toString()
        }
        if(output.contentType) {
            output.type = getTypeFromContentTypeString(output.contentType)
            output.fileName = "download." + output.contentType.tokenize("/").last()
        } else { // Auto detect contentType:
            switch (output.content) {
                case String:
                    String resStr = output.type.toString()
                    output.type = OutputType.TEXT
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
                            output.contentType = Mime.getType(new StringResource(resStr).inputStream) ?: "text/plain"
                            if(output.contentType == "text/plain") {
                                output.fileName = "download.txt"
                            } else {
                                output.fileName = "download." + output.contentType.tokenize("/").last()
                            }
                    }
                    break
                case File:
                    File file = output.type as File
                    output.contentType = Mime.getType(file)
                    output.type = getTypeFromContentTypeString(output.contentType)
                    output.fileName = file.name
                    break
                case BufferedImage:
                    BufferedImage img = output.content as BufferedImage
                    boolean hasAlpha = img.colorModel.hasAlpha()
                    String ext = hasAlpha ? "png" : "jpg"
                    output.type = OutputType.IMAGE
                    output.contentType = Mime.getType(ext)
                    output.fileName = "download.${ext}"
                    break
                case List:
                case Map:
                    output.type = OutputType.JSON
                    output.contentType = Mime.getType("json")
                    output.fileName = "download.json"
                    break
                case URL:
                    URL url = output.type as URL
                    HttpURLConnection conn = url.openConnection() as HttpURLConnection
                    conn.setRequestMethod("GET")
                    conn.connect()
                    output.contentType = conn.contentType
                    output.content = conn.content
                    output.responseCode = conn.responseCode
                    output.type = getTypeFromContentTypeString(output.contentType)
                    output.fileName = url.file
                    return output // Do not proceed to prevent changing content

                    break
                default:
                    output.type = OutputType.BINARY
                    output.contentType = "" //Unknown type
                    output.fileName = "download.bin"
            }
        }
        // Set content
        switch (output.type) {
            case OutputType.TEXT:
                switch (output.content) {
                    case File :
                        File file = output.content as File
                        output.content = file.text
                        output.size = file.size()
                        output.etag = file.lastModified().toString()
                        break
                    default:
                        output.content = output.content.toString()
                        output.size = (output.content as String).size()
                }
                break
            case OutputType.JSON:
                switch (output.content) {
                    case Collection:
                    case Map:
                        output.content = JSON.encode(output.content)
                        output.size = (output.content as String).size()
                        break
                }
                break
            case OutputType.YAML:
                switch (output.content) {
                    case Collection:
                    case Map:
                        output.content = new Yaml().dump(output.content)
                        output.size = (output.content as String).size()
                        break
                }
                break
            case OutputType.IMAGE:
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
                output.type = OutputType.BINARY
                output.size = (output.content as byte[]).length
                break
            case OutputType.BINARY:
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
        String query = request.queryString()
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
                    Log.v("Requested: %s By: %s", URLDecoder.decode(request.url(), "UTF-8"), request.ip())
                    Output output

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
                        HttpServletRequest raw = request.raw()
                        String tempDir = SysInfo.getTempDir()
                        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempDir))
                        boolean hasParts = false
                        try {
                            hasParts = !raw.parts.empty
                        } catch(Exception ignored) {}
                        // If we have uploads:
                        if (hasParts) {
                            File tempFileDir = SysInfo.getFile(tempDir)
                            if (tempFileDir.canWrite()) {
                                List<UploadFile> uploadFiles = []
                                if (raw.contentLength > 0) {
                                    raw.parts.each {
                                        Part part ->
                                            if(part.size) {
                                                try {
                                                    InputStream input = part.getInputStream()
                                                    Path path = Files.createTempFile("upload", ".file")
                                                    Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                                                    UploadFile file = new UploadFile(path.toString(), part.submittedFileName, part.name)
                                                    uploadFiles << file
                                                } catch (Exception e) {
                                                    response.status(500)
                                                    Log.e("Unable to upload file: %s", part.submittedFileName, e)
                                                }
                                            } else {
                                                response.status(500)
                                                Log.w("File: %s was empty", part.submittedFileName)
                                            }
                                    }
                                    try {
                                        Object res = callAction(sp.action, request, response, uploadFiles)
                                        output = handleContentType(res, sp.contentType)
                                    } catch (Exception e) {
                                        response.status(500)
                                        Log.e("Service.upload closure failed", e)
                                    }
                                    uploadFiles.each {
                                        if (it.exists()) {
                                            it.delete()
                                        }
                                    }
                                } else {
                                    response.status(411)
                                    Log.e("Uploaded file is empty")
                                }
                            } else {
                                response.status(503)
                                Log.e("Temporally directory %s is not writable", tempDir)
                            }
                        } else if (sp.cacheTime) { // Check if its in Cache
                            output = cache.get(getCacheKey(request), {
                                Output toSave = null
                                try {
                                    Object res = callAction(sp.action, request, response)
                                    toSave = handleContentType(res, sp.contentType)
                                } catch (Exception e) {
                                    response.status(500)
                                    Log.e("Service.action CACHE closure failed", e)
                                }
                                return toSave
                            }, sp.cacheTime)
                        } else { // Normal requests: (no cache, no file upload)
                            try {
                                Object res = callAction(sp.action, request, response)
                                output = handleContentType(res, sp.contentType)
                            } catch (Exception e) {
                                response.status(500)
                                Log.e("Service.action closure failed", e)
                            }
                        }

                        // ------------------- After content is processed ----------------
                        if(output) {
                            // Apply content-type:
                            if(output.contentType) {
                                response.type(output.contentType)
                            }
                            // Pass response code from output:
                            if(output.responseCode) {
                                response.status(output.responseCode)
                            }

                            // Set download
                            if (sp.download || output.contentType == "application/octet-stream") {
                                String fileName = sp.downloadFileName ?: output.fileName
                                response.header("Content-Disposition", "attachment; filename=" + fileName)
                                if (output.type == OutputType.BINARY) {
                                    response.header("Content-Transfer-Encoding", "binary")
                                }
                            }

                            // Set ETag:
                            if (sp.etag != null) {
                                String etag = sp.etag.calc(output.content) ?: output.etag
                                if (etag) {
                                    output.etag = etag
                                    response.header("ETag", etag)
                                } else {
                                    try {
                                        if (output.type == OutputType.BINARY) {
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
                                        } else {
                                            Log.v("Unable to generate ETag for: %s, unknown reason", request.uri())
                                        }
                                    } catch (Exception e) {
                                        //Can't be converted to String
                                        Log.v("Unable to set ETag for: %s, failed : %s", request.uri(), e.message)
                                    }
                                }
                            }

                            // Set content-length and Gzip headers
                            if (output.type != OutputType.BINARY && request.headers().size() > 0 && request.headers("Accept-Encoding")?.contains("gzip")) {
                                response.header("Content-Encoding", "gzip")
                                //TODO: Spark does not calculate size of gzip automatically:
                                // https://stackoverflow.com/questions/56404858/
                            } else {
                                response.header("Content-Length", sprintf("%d", output.size))
                            }
                        } else {
                            response.status(404)
                            output.content = "Not Found"
                        }
                    } else { // Unauthorized
                        response.status(403)
                        if(output == null) {
                            output = new Output(contentType: sp.contentType, type: getTypeFromContentTypeString(sp.contentType))
                        }
                        switch (output.type) {
                            case OutputType.JSON:
                                output.content = JSON.encode(ok : false, error : 403)
                                break
                            case OutputType.YAML:
                                output.content = new Yaml().dump(ok : false, error : 403)
                                break
                            default:
                                response.type(Mime.getType("txt"))
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
     * Adds Services to the controller
     * @param srv : Serviciable implementation
     */
    void addService(Serviciable srv) {
        if (!running) {
            if (srv instanceof ServiciableWebSocket || srv instanceof ServiciableHTTPS) {
                listServices.add(0, srv)
            } else if(srv) {
                listServices << srv
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

}
