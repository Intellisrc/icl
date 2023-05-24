package com.intellisrc.web

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import com.intellisrc.web.service.KeyStore
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Response
import groovy.transform.CompileStatic

import java.time.ZonedDateTime

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
    public File accessLog = File.get("log", "access.log")
    public boolean log = true
    public KeyStore ssl = null // Key Store File location and password (For WSS and HTTPS)
    public int timeout = Millis.MIN_10

    void log(Request request, Response response) {
        if(accessLog) {
            if(accessLog.parentFile.canWrite()) {
                ZonedDateTime now = SysClock.now.atZone(SysClock.clock.zone)
                String query = request.queryString
                accessLog << String.format("%s - - [%s] \"%s %s %s\" %d %d \"-\" \"%s\"\n",
                    request.ip,
                    now.format("dd/MMM/yyyy:HH:mm:ss Z"),
                    request.method.toUpperCase(),
                    request.requestURI + (query ? "?" + query : ""),
                    request.protocol,
                    response.status,
                    response.length,
                    request.userAgent
                )
            } else {
                Log.w("Unable to write access log in: %s", accessLog.parentFile.absolutePath)
            }
        }
    }

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
}
