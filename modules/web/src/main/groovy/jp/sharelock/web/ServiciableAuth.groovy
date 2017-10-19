package jp.sharelock.web

import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/03.
 */
interface ServiciableAuth extends Serviciable {
    String getLoginPath()
    String getLogoutPath()
    HashMap<String, Object> onLogin(Request request)
    boolean onLogout()
}