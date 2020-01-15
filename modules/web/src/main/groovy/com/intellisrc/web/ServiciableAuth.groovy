package com.intellisrc.web

import groovy.transform.CompileStatic
import spark.Request

@CompileStatic
/**
 * @since 17/04/03.
 */
interface ServiciableAuth extends Serviciable {
    String getLoginPath()
    String getLogoutPath()
    Map<String, Object> onLogin(Request request)
    boolean onLogout()
}