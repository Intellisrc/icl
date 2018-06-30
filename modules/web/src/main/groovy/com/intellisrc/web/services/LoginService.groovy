package com.intellisrc.web.services

import com.intellisrc.core.Log
import com.intellisrc.web.Service

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
import spark.Request

@groovy.transform.CompileStatic
class LoginService implements com.intellisrc.web.ServiciableAuth {
    String rootPath  = ""
    String loginPath = "/login"
    String logoutPath = "/logout"

    static enum Level {
        GUEST, USER, MODERATOR, ADMIN
    }

    Level getUserLevel(Request request) {
        return request.session().attribute("level").toString().toUpperCase() as Level
    }

    static final Service.Allow User = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.USER
            } else {
                return false
            }
    } as Service.Allow
    static final Service.Allow Moderator = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.MODERATOR
            } else {
                return false
            }
    } as Service.Allow
    static final Service.Allow Admin = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.ADMIN
            } else {
                return false
            }
    } as Service.Allow

    /**
     * This interface will include full request
     */
    interface LoginAction {
        Map<String,Object> call(Request request)
    }
    /**
     * This interface will only send login auth information
     */
    interface LoginAuth {
        Level call(String user, String password)
    }

    //Variables
    LoginAction onLoginAction = new LoginAction() {
        @Override
        Map<String, Object> call(Request request) {
            def user = request.queryParams("user") ?:
                       request.queryParams("usr") ?:
                       request.queryParams("u") ?: ""
            def pass = request.queryParams("pass") ?:
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
        }
    }

    /**
     * This property must be defined to allow login
     */
    LoginAuth onLoginAuth = new LoginAuth() {
        @Override
        Level call(String user, String password) {
            Log.e( "No login authorization has been implemented.")
            return Level.GUEST
        }
    }

    /**
     * Is recommended to override this method in child classes.
     * @param request
     * @return
     */
    @Override
    Map<String,Object> onLogin(final Request request) {
        return onLoginAction.call(request)
    }

    @Override
    String getPath() { rootPath }

    @Override
    String getLoginPath() { loginPath }

    @Override
    String getLogoutPath() { logoutPath }

    @Override
    boolean onLogout() {
        return true
    }

}
