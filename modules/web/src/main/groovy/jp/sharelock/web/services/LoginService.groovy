package jp.sharelock.web.services

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
    static final boolean levelUser = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.USER
            } else {
                return false
            }
    } as Allow
    static final boolean levelModerator = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.MODERATOR
            } else {
                return false
            }
    } as Allow
    static final boolean levelAdmin = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.ADMIN
            } else {
                return false
            }
    } as Allow

    interface LoginAction {
        Map<String,Object> call(Request request)
    }

    LoginAction onLoginAction
    LoginService(LoginAction callback) {
        onLoginAction = callback
    }

    @Override
    Map<String,Object> onLogin(final Request request) {
        return onLoginAction.call(request)
    }

    //Needs to be override but it is not used:
    @Override
    String getPath() { "" }

    @Override
    String getLoginPath() {
        "/login"
    }

    @Override
    String getLogoutPath() {
        "/logout"
    }

    @Override
    boolean onLogout() {
        return true
    }

}
