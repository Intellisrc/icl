package com.intellisrc.web

import groovy.transform.CompileStatic

@CompileStatic
/**
 * @since 17/04/03.
 */
interface ServiciableAuth extends Serviciable {
    String getLoginPath()
    String getLogoutPath()
    Map<String, Object> onLogin(Request request, Response response)
    boolean onLogout(Request request, Response response)
}