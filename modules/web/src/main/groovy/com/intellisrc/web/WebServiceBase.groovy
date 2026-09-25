package com.intellisrc.web

import com.intellisrc.core.Config
import com.intellisrc.core.Millis
import com.intellisrc.web.service.KeyStore
import com.intellisrc.web.service.Request
import com.intellisrc.web.tools.AccessLog
import groovy.transform.CompileStatic
/**
 * This is the common class between HTTP and WebSocket services
 * @since 2023/05/24.
 */
@CompileStatic
abstract class WebServiceBase {
    protected boolean initialized = false
    protected boolean running = false
    // Options:
    public Inet4Address address = "0.0.0.0".toInet4Address()
    public int port = 80
    public KeyStore ssl = null // Key Store File location and password (For WSS and HTTPS)
    public int timeout = Millis.MIN_10
    public AccessLog logger = new AccessLog()
    public boolean log = Config.any.get("web.log", false) //Turn to true to save access logs automatically

    public final File logDir = Config.any.getFile("web.log.dir", File.get(Config.any.get("log.path", "log")))
    protected File accessLogFile      = Config.any.getFile("web.log.access", File.get(logDir, "access.log"))
    protected File warnLogFile        = Config.any.getFile("web.log.warn", File.get(logDir, "warn.log"))
    protected File notFoundLogFile    = Config.any.getFile("web.log.notfound", File.get(logDir, "notfound.log"))

    boolean isSecure() {
        return ssl?.valid
    }

    boolean isRunning() {
        return this.running
    }

    boolean isStarted() {
        return this.running
    }

    boolean isStarting() {
        return this.initialized
    }

    boolean isStopping() {
        return ! this.running
    }

    boolean isStopped() {
        return ! this.running
    }

    boolean isFailed() {
        return initialized &&! this.running
    }
    /**
     * Set path for access log
     * @param path
     */
    void setAccessLog(Object path) {
        this.accessLogFile = path ? (path instanceof String &&! path.contains("/") ? File.get(logDir, path) : File.get(path)) : null
    }
    /**
     * Set path for warn log
     * @param path
     */
    void setWarnLog(Object path) {
        this.warnLogFile = path ? (path instanceof String &&! path.contains("/") ? File.get(logDir, path) : File.get(path)) : null
    }
    /**
     * Set path for not found log
     * @param path
     */
    void setNotFoundLog(Object path) {
        this.notFoundLogFile = path ? (path instanceof String &&! path.contains("/") ? File.get(logDir, path) : File.get(path)) : null
    }

    /**
     * Log a client access
     * @param request
     */
    void logAccess(Request request) {
        if(log && accessLogFile != null) {
            logger.access(accessLogFile, request)
        }
    }
    /**
     * Log some warning (request error)
     * @param request
     * @param code
     */
    void logWarn(Request request, int code) {
        if(log && warnLogFile != null) {
            logger.warn(warnLogFile, request, code)
        }
    }
    /**
     * Log not found requests
     * @param request
     */
    void logNotFound(Request request) {
        if(log && notFoundLogFile != null) {
            logger.notFound(notFoundLogFile, request)
        }
    }
    /**
     * Log successful logins
     * @param logFile
     * @param request
     */
    void logLogin(File logFile, Request request) {
        if(log && logFile != null) {
            logger.logged(logFile, request)
        }
    }
    /**
     * Log failed login attempt
     * @param logFile
     * @param request
     */
    void logFailLogin(File logFile, Request request) {
        if(log && logFile != null) {
            logger.failed(logFile, request)
        }
    }
    /**
     * Log logout
     * @param logFile
     * @param request
     */
    void logLogout(File logFile, Request request) {
        if(log && logFile != null) {
            logger.logged(logFile, request, true)
        }
    }
}
