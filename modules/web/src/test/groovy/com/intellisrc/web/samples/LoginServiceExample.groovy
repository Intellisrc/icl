package com.intellisrc.web.samples

/**
 * @since 17/04/19.
 */
import com.intellisrc.web.Service.Allow
import com.intellisrc.web.ServiciableAuth
import groovy.transform.CompileStatic
import spark.Request
import spark.Response

@CompileStatic
/**
 * @since 17/04/03.
 */
class LoginServiceExample implements ServiciableAuth {

    static enum Level {
        GUEST, USER, EDITOR, ADMIN
    }
    static final Allow canEditEmails = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.EDITOR
            } else {
                return false
            }
    } as Allow
    static final Allow isAdmin = {
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

    /**
     * In extended classes, overriding "onLogin" its the recommended
     * way to specify the login Task.
     *
     * However, you can override the "onLoginAction" parameter o
     * implemented it outside this class by setting on the constructor:
     *
     * addService(new LoginServiceExample(onLoginAction : {
     *     ...
     * } as LoginAction)
     *
     */
    @Override
    Map onLogin(Request request, Response response) {
        if(canLogin.check(request)) {
            String user = request.queryParams("my-user")
            String pass = request.queryParams("my-pass")
            if (user == "test" && pass == "test") {
                //This information will be stored in the session:
                return [
                        user : "test",
                        level: Level.USER,
                        name : "Super User Name"
                ]
            }
        }
        response.status(401)
        return [:]
    }

    @Override
    boolean onLogout(Request request, Response response) {
        request?.session()?.invalidate()
        return true
    }

    @Override
    String getPath() {
        "/auth"
    }

    @Override
    String getLoginPath() {
        ".login"        //becomes: /auth.login
    }

    @Override
    String getLogoutPath() {
        ".logout"       //becomes: /auth.logout
    }

}