package com.intellisrc.web.tools

import com.intellisrc.core.SysClock
import com.intellisrc.web.service.Request
import groovy.transform.CompileStatic

/**
 * This class will export access logs
 * Override '*Format' fields to replace output format or
 * override methods to replace output order, content or date/time format
 * @since 2024/04/10.
 */
@CompileStatic
class AccessLog {
    String accessFormat = "%s %s %s %s (%s) >> %s"
    String warnFormat = "%s %s %d %s %s (%s) >> %s"
    String notFoundFormat = "%s %s %s (%s)"
    String loggedFormat = "%s %s %s >> %s"
    String failedFormat = "%s %s %s %s [%s]"
    void access(File accessLogFile, Request request) {
        accessLogFile << String.format("$accessFormat\n", SysClock.now.YMDHmsS, request.method, request.ip, request.uri(), request.headers("Referer") ?: "no-referer", request.userAgent)
    }
    void warn(File warnLogFile, Request request, int errorCode) {
        warnLogFile << String.format("$warnFormat\n", SysClock.now.YMDHmsS, request.method, errorCode, request.ip, request.uri(), request.headers("Referer") ?: "no-referer", request.userAgent)
    }
    void notFound(File notFoundLogFile, Request request) {
        notFoundLogFile << String.format("$notFoundFormat\n", SysClock.now.YMDHmsS, request.ip, request.uri(), request.headers("Referer") ?: "no-referer")
    }
    void logged(File authLogFile, Request request, boolean out = false) {
        authLogFile << String.format("$loggedFormat\n", SysClock.now.YMDHmsS, out ? "OUT" : "IN ", request.ip, request.userAgent)
    }
    void failed(File authFailedLogFile, Request request) {
        authFailedLogFile << String.format("$failedFormat\n", SysClock.now.YMDHmsS, request.method, request.ip, request.uri(), request.userAgent)
    }
}
