package com.intellisrc.web.samples

import com.intellisrc.web.Service

/**
 * @since 17/04/19.
 */
import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/03.
 */
class LoginServiceExample implements com.intellisrc.web.ServiciableAuth {

    static enum Level {
        GUEST, USER, EDITOR, ADMIN
    }
    static final Service.Allow canEditEmails = {
        Request request ->
            if(request.session()) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.EDITOR
            } else {
                return false
            }
    } as Service.Allow
    static final Service.Allow isAdmin = {
        Request request ->
            if(!request.session().new) {
                Level level = request.session().attribute("level").toString().toUpperCase() as Level
                return level >= Level.ADMIN
            } else {
                return false
            }
    } as Service.Allow
    static final Service.Allow canLogin = {
        Request request ->
            println "IP: "+request.ip()
            return request.ip() ==~ /^127.0.0.1/
    } as Service.Allow

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
    Map onLogin(Request request) {
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
        return [:]
    }

    @Override
    boolean onLogout() {
        return false
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