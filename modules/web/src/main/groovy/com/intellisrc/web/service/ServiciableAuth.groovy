package com.intellisrc.web.service


import groovy.transform.CompileStatic

@CompileStatic
/**
 * @since 17/04/03.
 */
trait ServiciableAuth extends Serviciable {
    abstract String getLoginPath()
    abstract String getLogoutPath()
    abstract AuthData onLogin(Request request, Response response)
    boolean onLogout(Request request, Response response) {
        request?.session?.invalidate()
        return true
    }
}