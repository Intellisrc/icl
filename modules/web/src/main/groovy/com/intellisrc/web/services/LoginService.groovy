package com.intellisrc.web.services

import com.intellisrc.core.Log
import com.intellisrc.web.Service.Allow
import com.intellisrc.web.ServiciableAuth
import spark.Request
import spark.Response

/**
 * Generic class to allow access to private content
 * If this class doesn't fit your case, extend it (recommended) or imitate it
 *
 * onLoginAction, should be defined to allow login. The recommended way
 * to define it is on creation:
 *
 * addService(new LoginService(onLoginAction : {
 *     ...
 * } as LoginAction)
 *
 * TODO: redesign this class. Its not flexible enough. If possible, use:
 * https://github.com/pac4j/spark-pac4j
 *
 * @since 10/19/17.
 */

@groovy.transform.CompileStatic
class LoginService implements ServiciableAuth {
    String rootPath  = ""
    String loginPath = "/login"
    String logoutPath = "/logout"

    static enum Level {
        GUEST, USER, MODERATOR, ADMIN
    }

    static Level getUserLevel(Request request) {
        return (request?.session()?.attribute("level") ?: "GUEST").toString().toUpperCase() as Level
    }

    static final Allow User = {
        Request request ->
            if(request.session()) {
                return getUserLevel(request) >= Level.USER
            } else {
                return false
            }
    } as Allow
    static final Allow Moderator = {
        Request request ->
            if(request.session()) {
                return getUserLevel(request) >= Level.MODERATOR
            } else {
                return false
            }
    } as Allow
    static final Allow Admin = {
        Request request ->
            if(request.session()) {
                return getUserLevel(request) >= Level.ADMIN
            } else {
                return false
            }
    } as Allow

    /**
     * This interface will include full request
     */
    static interface LoginAction {
        Map<String,Object> call(Request request)
    }
    /**
     * This static interface will only send login auth information
     */
    static interface LoginAuth {
        Level call(String user, String password)
    }

    //Variables
    LoginAction onLoginAction = {
        Request request ->
            String user = request.queryParams("user") ?:
                       request.queryParams("usr") ?:
                       request.queryParams("u") ?: ""
            String pass = request.queryParams("pass") ?:
                       request.queryParams("password") ?:
                       request.queryParams("pwd") ?:
                       request.queryParams("p") ?: ""
            Level level = Level.GUEST
            if(user.isEmpty() || pass.isEmpty()) {
                Log.w("User or Password is empty. You can use onLoginAction interface or specify any standard query names")
            } else {
                level = onLoginAuth.call(user, pass) ?: Level.GUEST
            }
            return [
                user : user,
                level : level,
                login : level > Level.GUEST
            ] as Map<String, Object>
    } as LoginAction

    /**
     * This property must be defined to allow login
     */
    LoginAuth onLoginAuth = {
        String user, String password ->
            Log.e( "No login authorization has been implemented.")
            return Level.GUEST
    } as LoginAuth

    /**
     * Is recommended to override this method in child classes.
     * @param request
     * @return
     */
    @Override
    Map<String,Object> onLogin(final Request request, final Response response) {
        return onLoginAction.call(request)
    }

    @Override
    String getPath() { rootPath }

    @Override
    String getLoginPath() { loginPath }

    @Override
    String getLogoutPath() { logoutPath }

    @Override
    boolean onLogout(final Request request, final Response response) {
        return request?.session()?.invalidate()
    }

}
