package jp.sharelock.web

import jp.sharelock.etc.CacheObj
import jp.sharelock.etc.Log
import spark.Request
import spark.Response
import spark.Route
import spark.Service

import static jp.sharelock.web.ServicePath.Method.*
import static groovy.json.JsonOutput.toJson

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
    public static String LOG_TAG = WebService.simpleName
    private final Service srv
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
        srv = Service.ignite()
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
        Log.i(LOG_TAG, "Starting server in port $port with pool size of $threads")
        listServices.each {
            Serviciable serviciable ->
                switch(serviciable) {
                    case ServiciableMultiple:
                        ServiciableMultiple serviciables = serviciable as ServiciableMultiple
                        serviciables.services.each {
                            ServicePath sp ->
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
                        break
                    default:
                        Log.e(LOG_TAG, "Interface not implemented")
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
                                return toJson(
                                        y : ok,
                                        id : id
                                )
                        })
                        srv.get(auth.path + auth.logoutPath, {
                            Request request, Response response ->
                                response.type("application/json")
                                request.session().invalidate()
                                return toJson(
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
    private void addServicePath(ServicePath service, String rootPath) {
        service.fullPath = rootPath + service.path
        if (listPaths.contains(service.method.toString() + service.fullPath)) {
            Log.w(LOG_TAG, "Warning, duplicated path ["+service.fullPath+"] and method [" + service.method.toString() + "] found.")
        } else {
            listPaths << service.method.toString() + service.fullPath
            addAction(service.fullPath, service)
        }
    }

    /**
     * stop web service
     */
    void stop() {
        Log.i(LOG_TAG, "Stopping server running at port: $port")
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
     * @param sp       : ServicePath object
     * @param srv      : Spark.Service instance
     */
    private void addAction(final String fullPath, final ServicePath sp) {
        //srv."$method"(fullPath, onAction(sp)) //Dynamic method invocation: will call srv.get, srv.post, etc (not supported with CompileStatic
        switch(sp.method) {
            case GET:       srv.get(fullPath, onAction(sp));     break
            case POST:      srv.post(fullPath, onAction(sp));    break
            case PUT:       srv.put(fullPath, onAction(sp));     break
            case DELETE:    srv.delete(fullPath, onAction(sp));  break
            case OPTIONS:   srv.options(fullPath, onAction(sp)); break
        }
    }

    /**
     * Returns the Route to be added into Spark.Service based in ServicePath object
     * @param sp : ServicePath object
     * @return
     */
    private static Route onAction(final ServicePath sp) {
        return {
            Request request, Response response ->
                String out
                response.type("application/json")
                if(sp.allow.check(request)) {
                    if(sp.cacheTime) {
                        String key = sp.fullPath
                        out = CacheObj.instance.get(key, {
                            toJson(sp.action.run(request))
                        }, sp.cacheTime)
                    } else {
                        out = toJson(sp.action.run(request))
                    }
                } else {
                    out = toJson(y : false)
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
