package jp.sharelock.web.samples

/**
 * @since 17/04/19.
 */
import jp.sharelock.web.ServiciableAuth
import jp.sharelock.web.ServicePath.Allow
import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/03.
 */
class LoginService implements ServiciableAuth {

    static enum Level {
        GUEST, USER, EDITOR, ADMIN
    }
    static final Allow  canEditEmails = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.EDITOR
            } else {
                return false
            }
    } as Allow
    static final Allow  isAdmin = {
        Request request ->
            if(!request.session().new) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.ADMIN
            } else {
                return false
            }
    } as Allow
    static final Allow canLogin = {
        Request request ->
            println "IP: "+request.ip()
            return request.ip() ==~ /^127.0.0.1/
    } as Allow

    @Override
    String getPath() {
        ""
    }

    @Override
    String getLoginPath() {
        "/login"
    }

    @Override
    String getLogoutPath() {
        "/logout"
    }

    @Override
    HashMap<String,Object> onLogin(final Request request) {
        if(canLogin.check(request)) {
            String user = request.queryParams("user")
            String pass = request.queryParams("pass")
            if (user == "test" && pass == "test") {
                return [
                        user : "test",
                        level: Level.ADMIN,
                        name : "Super User"
                ]
            }
        }
        return [:]
    }

    @Override
    boolean onLogout() {
        return true
    }

}