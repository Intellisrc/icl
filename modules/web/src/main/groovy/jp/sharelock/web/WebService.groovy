package jp.sharelock.web

import jp.sharelock.etc.CacheObj
import jp.sharelock.etc.Log
import jp.sharelock.etc.SysInfo
import static jp.sharelock.web.Service.Method.*

import spark.Request
import spark.Response
import spark.Route
import spark.Service as Srv

import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletResponse
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
    private ArrayList<Serviciable> listServices = []
    private ArrayList<String> listPaths = [] //mainly used to prevent collisions
    private boolean resourcesAbsolute = false
    // Options:
    String resources = ""
    int cacheTime = 0
    int port = 80
    int threads = 20
    private boolean running = false

    /**
     * Constructor: initializes Service instance
     */
    WebService() {
        srv = Srv.ignite()
    }

    /**
     * start web service
     * //TODO: fix paths so path1 + path2 is always correct
     */
    void start(boolean daemon = true) {
        srv.staticFiles.expireTime(cacheTime)
        if(!resources.isEmpty()) {
            if (resourcesAbsolute) {
                srv.staticFiles.externalLocation(resources)
            } else {
                srv.staticFiles.location(resources)
            }
        }
        srv.port(port).threadPool(threads) //Initialize it right away
        Log.i( "Starting server in port $port with pool size of $threads")
        listServices.each {
            Serviciable serviciable ->
                switch(serviciable) {
                    case ServiciableMultiple:
                        ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                        serviciables.services.each {
                            Service sp ->
                                if(serviciable instanceof ServiciableWebSocket) {
                                    addWebSocketService(serviciable, serviciables.path + sp.path)
                                } else if(serviciable instanceof ServiciableHTTPS) {
                                    addSSLService(serviciable)
                                    addServicePath(sp, serviciables.path)
                                } else {
                                    addServicePath(sp, serviciables.path)
                                }
                        }
                        break
                    case ServiciableSingle:
                        ServiciableSingle single = serviciable as ServiciableSingle
                        if(serviciable instanceof ServiciableWebSocket) {
                            addWebSocketService(single, single.path)
                        } else if(serviciable instanceof ServiciableHTTPS) {
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
                        Log.e( "Interface not implemented")
                }
                switch(serviciable) {
                    case ServiciableAuth:
                        ServiciableAuth auth = serviciable as ServiciableAuth
                        srv.post(auth.path + auth.loginPath, {
                            Request request, Response response ->
                                def ok = false
                                def sessionMap = auth.onLogin(request)
                                def id = 0
                                if(!sessionMap.isEmpty()) {
                                    ok = true
                                    request.session(true)
                                    sessionMap.each {
                                        request.session().attribute(it.key, it.value)
                                    }
                                    id = request.session().id()
                                }
                                response.type("application/json")
                                return JSON.encode(
                                        y : ok,
                                        id : id
                                )
                        })
                        srv.get(auth.path + auth.logoutPath, {
                            Request request, Response response ->
                                response.type("application/json")
                                request.session().invalidate()
                                return JSON.encode(
                                        y : auth.onLogout()
                                )
                        })
                        break
                }
        }
        srv.init()
        running = true
        if(!daemon) {
            while(running) {
                sleep 1000L
            }
        }
        //Wait until the server is Up
        sleep 1000L
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
    private static Object getOutput(Response response, Object output, OutputType otype) {
        Object out
        switch(otype) {
            case OutputType.JSON:
                out = JSON.encode(output)
                break
            case OutputType.BINARY:
                HttpServletResponse raw = response.raw()
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
                raw.getOutputStream().write(content)
                raw.getOutputStream().flush()
                raw.getOutputStream().close()
                raw.setContentLength(content.length)
                out = raw
                break
            case OutputType.PLAIN:
            default:
                out = output.toString()
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
                Object out = ""
                OutputType otype = sp.contentType.contains("json") ? OutputType.JSON : (sp.contentType.contains("text") ? OutputType.PLAIN : OutputType.BINARY)
                response.type(sp.contentType)
                sp.headers.each {
                    String key, String val ->
                        response.header(key, val)
                }
                if(sp.allow.check(request)) {
                    if(sp.download) {
                        response.header("Content-Disposition", "attachment; filename="+sp.download)
                    }
                    if(otype == OutputType.BINARY) {
                        response.header("Content-Transfer-Encoding", "binary")
                    }
                    if(sp.upload) {
                        def tempDir = SysInfo.getTempDir()
                        def tempFileDir = new File(tempDir)
                        if(tempFileDir.canWrite()) {
                            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempDir))
                            Path path = Files.createTempFile("upload", ".file")
                            def raw = request.raw()
                            if(raw.contentLength > 0) {
                                try {
                                    def part = raw.getPart(sp.uploadField)
                                    if(part) {
                                        InputStream input = part.getInputStream()
                                        Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
                                        def file = new File(path.toString())
                                        try {
                                            Object res
                                            switch(sp.upload) {
                                                case Service.UploadRequestResponse: res = (sp.upload as Service.UploadRequestResponse).run(file, request, response); break
                                                case Service.UploadRequest: res = (sp.upload as Service.UploadRequest).run(file, request); break
                                                case Service.Upload:
                                                default: res = (sp.upload as Service.Upload).run(file); break
                                            }
                                            out = getOutput(response, res, otype)
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
                                        Log.e("Upload field name does not matches Service.uploadField value: %s",sp.uploadField)
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
                    } else if(sp.cacheTime) {
                        String query = request.queryString()
                        String key = request.uri() + (query  ? "?" + query : "")
                        out = CacheObj.instance.get(key, {
                            def toSave = ""
                            try {
                                Object res
                                switch(sp.action) {
                                    case Service.ActionRequestResponse: res = (sp.action as Service.ActionRequestResponse).run(request, response); break
                                    case Service.ActionRequest: res = (sp.action as Service.ActionRequest).run(request); break
                                    case Service.Action:
                                    default: res = (sp.action as Service.Action).run(); break
                                }
                                toSave = getOutput(response, res, otype)
                            } catch (Exception e) {
                                response.status(500)
                                Log.e("Service.action CACHE closure failed",e)
                            }
                            return toSave
                        }, sp.cacheTime)
                    } else {
                        try {
                            Object res
                            switch(sp.action) {
                                case Service.ActionRequestResponse: res = (sp.action as Service.ActionRequestResponse).run(request, response); break
                                case Service.ActionRequest: res = (sp.action as Service.ActionRequest).run(request); break
                                case Service.Action:
                                default: res = (sp.action as Service.Action).run(); break
                            }
                            out = getOutput(response, res, otype)
                        } catch (Exception e) {
                            response.status(500)
                            Log.e("Service.action closure failed",e)
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
        resourcesAbsolute = absolute
    }

}
