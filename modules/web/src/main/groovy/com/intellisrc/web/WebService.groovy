package com.intellisrc.web

import com.intellisrc.etc.CacheObj
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo

import static com.intellisrc.web.Service.Method.*

import spark.Request
import spark.Response
import spark.Route
import spark.Service as Srv

import javax.servlet.MultipartConfigElement
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@groovy.transform.CompileStatic
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
    private boolean embedded = false //Turn to true if resources are inside jar
    // Options:
    String resources = ""
    int cacheTime = 0
    int port = 80
    int threads = 20
    int eTagMaxKB = 1024
    private boolean running = false
    StartCallback onStart = null
    
    interface StartCallback {
        void call(Srv srv)
    }
    /**
     * Constructor: initializes Service instance
     */
    WebService() {
        srv = Srv.ignite()
    }

    /**
     * start web service
     */
    void start(boolean background = false) {
        srv.staticFiles.expireTime(cacheTime)
        if(!resources.isEmpty()) {
            if (embedded) {
                srv.staticFiles.location(resources)
            } else {
                File resFile = SysInfo.getFile(resources)
                srv.staticFiles.externalLocation(resFile.absolutePath)
            }
        }
        if(NetworkInterface.isPortAvailable(port)) {
            srv.port(port).threadPool(threads) //Initialize it right away
            Log.i("Starting server in port $port with pool size of $threads")
            listServices.each {
                Serviciable serviciable ->
                    switch (serviciable) {
                        case ServiciableMultiple:
                            ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                            serviciables.services.each {
                                Service sp ->
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
                                    def ok = false
                                    def sessionMap = auth.onLogin(request)
                                    def id = 0
                                    if (!sessionMap.isEmpty()) {
                                        ok = true
                                        request.session(true)
                                        sessionMap.each {
                                            request.session().attribute(it.key, it.value)
                                        }
                                        id = request.session().id()
                                    }
                                    response.type("application/json")
                                    return JSON.encode(
                                            y: ok,
                                            id: id
                                    )
                            })
                            srv.get(auth.path + auth.logoutPath, {
                                Request request, Response response ->
                                    response.type("application/json")
                                    request.session().invalidate()
                                    return JSON.encode(
                                            y: auth.onLogout()
                                    )
                            })
                            break
                    }
            }
            srv.init()
            running = true
            if(onStart) {
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
        def fullPath = rootPath + service.path
        if (listPaths.contains(service.method.toString() + fullPath)) {
            Log.w( "Warning, duplicated path ["+fullPath+"] and method [" + service.method.toString() + "] found.")
        } else {
            listPaths << service.method.toString() + fullPath
            addAction(fullPath, service)
        }
    }

    /**
     * stop web service
     */
    void stop() {
        Log.i( "Stopping server running at port: $port")
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
     * @param sp       : Service object
     * @param srv      : Spark.Service instance
     */
    private void addAction(final String fullPath, final Service sp) {
        //srv."$method"(fullPath, onAction(sp)) //Dynamic method invocation: will call srv.get, srv.post, etc (not supported with CompileStatic
        //Automatic set POST for uploads
        if(sp.upload && sp.method == GET) {
            sp.method = POST
        }
        switch(sp.method) {
            case GET:       srv.get(fullPath, onAction(sp));     break
            case POST:      srv.post(fullPath, onAction(sp));    break
            case PUT:       srv.put(fullPath, onAction(sp));     break
            case DELETE:    srv.delete(fullPath, onAction(sp));  break
            case OPTIONS:   srv.options(fullPath, onAction(sp)); break
        }
    }
    /**
     * Output types used in getOutput
     */
    private final static enum OutputType {
        JSON, PLAIN, BINARY
    }
    /**
     * Gets the output and convert it
     * @param output
     * @param otype
     * @return
     */
    private static Object getOutput(Object output, OutputType otype) {
        Object out
        switch(otype) {
            case OutputType.JSON:
                out = JSON.encode(output)
                break
            case OutputType.BINARY:
                byte[] content
                switch (output) {
                    case File:
                        content = (output as File).bytes
                        break
                    case String:
                    default:
                        content = output.toString().bytes
                        break
                }
                out = content
                break
            case OutputType.PLAIN:
            default:
                switch (output) {
                    case File:
                        out = (output as File).text
                        break
                    case String:
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
    private static Route onAction(final Service sp) {
        return {
            Request request, Response response ->
                Log.v("Requested: %s By: %s", URLDecoder.decode(request.url(), "UTF-8"), request.ip())
                Object out = ""     // Output to serve
                Object res = null   // Response from Service
                OutputType otype = sp.contentType.contains("json") ? OutputType.JSON : (sp.contentType.contains("text") ? OutputType.PLAIN : OutputType.BINARY)
                response.type(sp.contentType)
                sp.headers.each {
                    String key, String val ->
                        response.header(key, val)
                }
                if(sp.allow.check(request)) {
                    if (sp.download) {
                        response.header("Content-Disposition", "attachment; filename=" + sp.download)
                        if (otype == OutputType.BINARY) {
                            response.header("Content-Transfer-Encoding", "binary")
                        }
                    }
                    //TODO: when its upload, its hard to work together with POST requests. Make it simpler
                    if (sp.upload) {
                        def tempDir = SysInfo.getTempDir()
                        def tempFileDir = new File(tempDir)
                        if (tempFileDir.canWrite()) {
                            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempDir))
                            Path path = Files.createTempFile("upload", ".file")
                            def raw = request.raw()
                            if (raw.contentLength > 0) {
                                try {
                                    def part = raw.getPart(sp.uploadField)
                                    if (part) {
                                        InputStream input = part.getInputStream()
                                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                                        def file = new File(path.toString())
                                        try {
                                            switch (sp.upload) {
                                                case Service.UploadRequestResponse: res = (sp.upload as Service.UploadRequestResponse).run(file, request, response); break
                                                case Service.UploadRequest: res = (sp.upload as Service.UploadRequest).run(file, request); break
                                                case Service.Upload:
                                                default: res = (sp.upload as Service.Upload).run(file); break
                                            }
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
                            def toSave = ""
                            try {
                                switch (sp.action) {
                                    case Service.ActionRequestResponse: res = (sp.action as Service.ActionRequestResponse).run(request, response); break
                                    case Service.ActionRequest: res = (sp.action as Service.ActionRequest).run(request); break
                                    case Service.Action:
                                    default: res = (sp.action as Service.Action).run(); break
                                }
                                toSave = getOutput(res, otype)
                            } catch (Exception e) {
                                response.status(500)
                                Log.e("Service.action CACHE closure failed", e)
                            }
                            return toSave
                        }, sp.cacheTime)
                    } else {
                        try {
                            switch (sp.action) {
                                case Service.ActionRequestResponse: res = (sp.action as Service.ActionRequestResponse).run(request, response); break
                                case Service.ActionRequest: res = (sp.action as Service.ActionRequest).run(request); break
                                case Service.Action:
                                default: res = (sp.action as Service.Action).run(); break
                            }
                            out = getOutput(res, otype)
                        } catch (Exception e) {
                            response.status(500)
                            Log.e("Service.action closure failed", e)
                        }
                    }
                    if (sp.noStore) { //Never store in client
                        response.header("Cache-Control", "no-store")
                    } else if (!sp.cacheTime &&! sp.maxAge) { //Revalidate each time
                        response.header("Cache-Control", "no-cache")
                    } else {
                        String priv = (sp.isPrivate) ? "private," : "" //User-specific data
                        response.header("Cache-Control", priv + "max-age=" + sp.maxAge)
                    }
                    if (sp.etag != null) {
                        String etag = sp.etag.calc(out)
                        if(etag) {
                            response.header("ETag", sp.etag.calc(out))
                        } else {
                            try {
                                if(res instanceof File) {
                                    etag = ((File) res).lastModified().toString()
                                } else if (otype == OutputType.BINARY) {
                                    if((out as byte[]).length > 1024 * eTagMaxKB) {
                                        Log.v("Unable to generate ETag for: %s, output is Binary, you can add 'etag' property in Service or increment 'eTagMaxKB' property to dismiss this message", request.uri())
                                    } else {
                                        etag = (out as byte[]).encodeHex().toString().md5()
                                    }
                                } else {
                                    etag = out.toString().md5()
                                }
                                if(etag) {
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
                    if (otype == OutputType.BINARY) {
                        if (res instanceof File) {
                            response.header("Content-Length", sprintf("%d", (res as File).size()))
                        } else {
                            response.header("Content-Length", sprintf("%d", (out as byte[]).length))
                        }
                    } else {
                        if(request.headers().size() > 0 && request.headers("Accept-Encoding")?.contains("gzip")) {
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
                    response.status(401)
                    out = otype == OutputType.JSON ? JSON.encode(y : false) : ""
                }
                return out
        } as Route
    }

    /**
     * Adds Services to the controller
     * @param srv : Serviciable implementation
     */
    void addService(Serviciable srv) {
        if(srv instanceof ServiciableWebSocket || srv instanceof ServiciableHTTPS) {
            listServices.add(0, srv)
        } else {
            listServices << srv
        }
    }

    /**
     * Add Resources
     * @param path
     * @param absolute : if set, absolute path will be allowed (not recommended due to security reasons)
     */
    void setResources(String path, boolean absolute = false) {
        resources = path
        embedded = absolute
    }

}
