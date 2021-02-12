package com.intellisrc.web

import com.intellisrc.etc.Cache
import com.intellisrc.etc.CacheObj
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import javax.imageio.ImageIO
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
    private final Srv srv
    private List<Serviciable> listServices = []
    private List<String> listPaths = [] //mainly used to prevent collisions
    private boolean running = false
    private String resources = ""
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
                                        res.y = ok
                                        return JSON.encode(res)
                                })
                                srv.get(auth.path + auth.logoutPath, {
                                    Request request, Response response ->
                                        response.type("application/json")
                                        request.session().invalidate()
                                        return JSON.encode(
                                                y: auth.onLogout(request, response)
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
    private void addWebSocketService(Serviciable serviciable, String path) {
        ServiciableWebSocket webSocket = serviciable as ServiciableWebSocket
        srv.webSocket(path, new WebSocketService(webSocket))
    }

    /**
     * Add SSL to connection
     * @param serviciable
     */
    private void addSSLService(Serviciable serviciable) {
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
    private void addServicePath(Service service, String rootPath) {
        String fullPath = rootPath + service.path
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
    private void addAction(final String fullPath, final Service sp) {
        //srv."$method"(fullPath, onAction(sp)) //Dynamic method invocation: will call srv.get, srv.post, etc (not supported with CompileStatic
        //Automatic set POST for uploads
        if (sp.uploadField && sp.method == GET) {
            sp.method = POST
            Log.w("%s is set to Upload a file, but GET method was used. Setting POST", fullPath)
        }
        switch (sp.method) {
            case GET: srv.get(fullPath, onAction(sp)); break
            case POST: srv.post(fullPath, onAction(sp)); break
            case PUT: srv.put(fullPath, onAction(sp)); break
            case DELETE: srv.delete(fullPath, onAction(sp)); break
            case OPTIONS: srv.options(fullPath, onAction(sp)); break
        }
    }
    /**
     * Output types used in getOutput
     */
    private final static enum OutputType {
        JSON, PLAIN, BINARY, JPG, PNG, GIF
    }
    /**
     * Gets the output and convert it
     * @param output
     * @param otype
     * @return
     */
    private static Object getOutput(Object output, OutputType otype) {
        Object out
        switch (otype) {
            case OutputType.JSON:
                out = JSON.encode(output)
                break
            case OutputType.JPG:
            case OutputType.PNG:
            case OutputType.GIF:
            case OutputType.BINARY:
                byte[] content
                switch (output) {
                    case File:
                        content = (output as File).bytes
                        break
                    case BufferedImage:
                        BufferedImage img = output as BufferedImage
                        if(otype == OutputType.BINARY) {
                            content = ((DataBufferByte) img.raster.dataBuffer).data
                        } else {
                            ByteArrayOutputStream os = new ByteArrayOutputStream()
                            ImageIO.write(img, otype.toString().toLowerCase(), os)
                            content = os.toByteArray()
                            os.close()
                        }
                        break
                    case String:
                        content = output.toString().bytes
                        break
                    default:
                        content = output as byte[]
                }
                out = content
                break
        //case OutputType.PLAIN:
            default:
                switch (output) {
                    case File:
                        out = (output as File).text
                        break
                //case String:
                    default:
                        out = output.toString()
                        break
                }
        }
        return out
    }

    /**
     * Returns the Route to be added into Spark.Service based in Service object
     * @param sp : Service object
     * @return
     */
    private Route onAction(final Service sp) {
        return {
            Request request, Response response ->
                try {
                    Log.v("Requested: %s By: %s", URLDecoder.decode(request.url(), "UTF-8"), request.ip())
                    Object out = ""     // Output to serve
                    Object res = null   // Response from Service
                    OutputType otype
                    switch (sp.contentType.toLowerCase()) {
                        case ~/.*json.*/  : otype = OutputType.JSON; break
                        case ~/.*text.*/  : otype = OutputType.PLAIN; break
                        case "image/jpeg" : otype = OutputType.JPG; break
                        case "image/png"  : otype = OutputType.PNG; break
                        case "image/gif"  : otype = OutputType.GIF; break
                        default:
                            otype = OutputType.BINARY
                    }
                    response.type(sp.contentType)
                    sp.headers.each {
                        String key, String val ->
                            response.header(key, val)
                    }
                    // Apply general allow origin rule:
                    if (allowOrigin) {
                        response.header("Access-Control-Allow-Origin", allowOrigin)
                    }
                    if (sp.allow.check(request)) {
                        if (sp.download) {
                            response.header("Content-Disposition", "attachment; filename=" + sp.download)
                            if (otype == OutputType.BINARY) {
                                response.header("Content-Transfer-Encoding", "binary")
                            }
                        }
                        if (sp.uploadField) {
                            String tempDir = SysInfo.getTempDir()
                            File tempFileDir = new File(tempDir)
                            if (tempFileDir.canWrite()) {
                                request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempDir))
                                Path path = Files.createTempFile("upload", ".file")
                                HttpServletRequest raw = request.raw()
                                if (raw.contentLength > 0) {
                                    try {
                                        Part part = raw.getPart(sp.uploadField)
                                        if (part) {
                                            InputStream input = part.getInputStream()
                                            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                                            File file = new File(path.toString())
                                            try {
                                                res = callAction(sp.action, request, response, file)
                                                out = getOutput(res, otype)
                                            } catch (Exception e) {
                                                response.status(500)
                                                Log.e("Service.upload closure failed", e)
                                            }
                                            if (file.exists()) {
                                                file.delete()
                                            }
                                            return out
                                        } else {
                                            response.status(500)
                                            Log.e("Upload field name does not matches Service.uploadField value: %s", sp.uploadField)
                                        }
                                    } catch (Exception e) {
                                        response.status(500)
                                        Log.e("Unable to upload file.", e)
                                    }
                                } else {
                                    response.status(411)
                                    Log.e("Uploaded file is empty")
                                }
                            } else {
                                response.status(503)
                                Log.e("Temporally directory %s is not writable", tempDir)
                            }
                        } else if (sp.cacheTime) {
                            String query = request.queryString()
                            String key = request.uri() + (query ? "?" + query : "")
                            out = CacheObj.instance.get(key, {
                                String toSave = ""
                                try {
                                    res = callAction(sp.action, request, response)
                                    toSave = getOutput(res, otype)
                                } catch (Exception e) {
                                    response.status(500)
                                    Log.e("Service.action CACHE closure failed", e)
                                }
                                return toSave
                            }, sp.cacheTime)
                        } else {
                            try {
                                res = callAction(sp.action, request, response)
                                out = getOutput(res, otype)
                            } catch (Exception e) {
                                response.status(500)
                                Log.e("Service.action closure failed", e)
                            }
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
                        if (sp.etag != null) {
                            String etag = sp.etag.calc(out)
                            if (etag) {
                                response.header("ETag", sp.etag.calc(out))
                            } else {
                                try {
                                    if (res instanceof File) {
                                        etag = ((File) res).lastModified().toString()
                                    } else if (otype == OutputType.BINARY || otype == OutputType.JPG || otype == OutputType.PNG) {
                                        if ((out as byte[]).length > 1024 * eTagMaxKB) {
                                            Log.v("Unable to generate ETag for: %s, output is Binary, you can add 'etag' property in Service or increment 'eTagMaxKB' property to dismiss this message", request.uri())
                                        } else {
                                            etag = (out as byte[]).encodeHex().toString().md5()
                                        }
                                    } else {
                                        etag = out.toString().md5()
                                    }
                                    if (etag) {
                                        response.header("ETag", etag)
                                    } else {
                                        Log.v("Unable to generate ETag for: %s, unknown reason", request.uri())
                                    }
                                } catch (Exception e) {
                                    //Can't be converted to String
                                    Log.v("Unable to set etag for: %s, failed : %s", request.uri(), e.message)
                                }
                            }
                        }
                        // Set content-length and Gzip headers
                        if (otype == OutputType.BINARY || otype == OutputType.JPG || otype == OutputType.PNG) {
                            if (res instanceof File) {
                                response.header("Content-Length", sprintf("%d", (res as File).size()))
                            } else {
                                response.header("Content-Length", sprintf("%d", (out as byte[]).length))
                            }
                        } else {
                            if (request.headers().size() > 0 && request.headers("Accept-Encoding")?.contains("gzip")) {
                                response.header("Content-Encoding", "gzip")
                                //TODO: https://stackoverflow.com/questions/56404858/
                            } else {
                                if (res instanceof File) {
                                    response.header("Content-Length", sprintf("%d", (res as File).size()))
                                } else {
                                    response.header("Content-Length", sprintf("%d", out.toString().length()))
                                }
                            }
                        }
                    } else {
                        response.status(403)
                        out = otype == OutputType.JSON ? JSON.encode(y: false) : ""
                    }
                    return out
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
    private static Object callAction(final Object action, final Request request, final Response response, final File upload = null) {
        Object returned = null
        if (action instanceof Service.Action) {
            returned = action.run(request, response)
        } else if (action instanceof Service.Upload) {
            returned = action.run(upload, request, response)
        } else if (action instanceof Closure) {
            if (upload) {
                switch (true) {
                    case tryCall(action, { returned = it }, upload, request, response): break
                    case tryCall(action, { returned = it }, request, response, upload): break
                    case tryCall(action, { returned = it }, upload, request): break
                    case tryCall(action, { returned = it }, upload, response): break
                    case tryCall(action, { returned = it }, request, upload): break
                    case tryCall(action, { returned = it }, response, upload): break
                    case tryCall(action, { returned = it }, upload): break
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
    private static boolean tryCall(Closure action, Closure returnValue, Object... params) {
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
            } else {
                listServices << srv
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
