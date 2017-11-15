package jp.sharelock.web.services

import jp.sharelock.etc.Log

/**
 * Generic class to allow access to private content
 * If this class doesn't fit your case, extend it or imitate it
 * @since 10/19/17.
 */
import jp.sharelock.web.ServiciableAuth
import jp.sharelock.web.ServicePath.Allow
import spark.Request

@groovy.transform.CompileStatic
class LoginService implements ServiciableAuth {
    static enum Level {
        GUEST, USER, MODERATOR, ADMIN
    }
    static final Allow User = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.USER
            } else {
                return false
            }
    } as Allow
    static final Allow Moderator = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.MODERATOR
            } else {
                return false
            }
    } as Allow
    static final Allow Admin = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.ADMIN
            } else {
                return false
            }
    } as Allow

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
    LoginAuth onLoginAuth = new LoginAuth() {
        @Override
        Level call(String user, String password) {
            Log.e( "No login authorization has been implemented.")
            return Level.GUEST
        }
    }
    String rootPath  = ""
    String loginPath = "/login"
    String logoutPath = "/logout"

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
