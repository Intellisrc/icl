package com.intellisrc.web.samples

import com.intellisrc.web.service.AuthData
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Response
import com.intellisrc.web.service.Service.Allow
import com.intellisrc.web.service.ServiciableAuth
import groovy.transform.CompileStatic

/**
 * @since 17/04/19.
 */
@CompileStatic
/**
 * @since 17/04/03.
 */
class LoginServiceExample implements ServiciableAuth {
    final String path = "/auth"
    final String loginPath = ".login"   //auth.login
    final String logoutPath = ".logout" //auth.logout

    static enum Level {
        GUEST, USER, EDITOR, ADMIN
    }
    static final Allow canEditEmails = {
        Request request ->
            if(request.session) {
                Level level = request.session.attribute("level").toString().toUpperCase() as Level
                return level >= Level.EDITOR
            } else {
                return false
            }
    } as Allow
    static final Allow isAdmin = {
        Request request ->
            if(!request.session.new) {
                Level level = request.session.attribute("level").toString().toUpperCase() as Level
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
    AuthData onLogin(Request request, Response response) {
        if(canLogin.check(request)) {
            String user = request.queryParams("my-user")
            String pass = request.queryParams("my-pass")
            if (user == "test" && pass == "test") {
                //This information will be stored in the session:
                return new AuthData(
                    toStoreInServer: [
                        user : "test",
                        level: Level.USER,
                        name : "Super User Name"
                    ]
                )
            }
        }
        response.status(401)
        return AuthData.empty
    }
}