package com.intellisrc.web.samples

import com.intellisrc.web.Request
import com.intellisrc.web.Service
import com.intellisrc.web.Service.Action
import com.intellisrc.web.Services

/**
 * @since 17/04/19.
 */

import groovy.transform.CompileStatic

import static com.intellisrc.web.Service.ActionRequest
import static com.intellisrc.web.Service.Method.POST

@CompileStatic
/**
 * @since 17/04/03.
 */
class EmailService extends Services {

    Action getEmails = {
        Request request ->
            return [
                    "admin@example.com",
                    "info@example.com",
                    "webmaster@example.com"
            ]
    } as ActionRequest

    Action saveEmails = {
        Request request ->
            return [
                    y : true
            ]
    } as ActionRequest

    Action getEmailsNew = {
        Request request ->
            return [
                    "new@example.com"
            ]
    } as ActionRequest

    Action getEmailsMore = {
        Request request ->
            println "We are here to see and hear..."
            return [
                    "new@example.com"
            ]
    } as ActionRequest

    /**
     * The root path exists to minimize the probability of URL collision
     * while can be empty (and thus all paths will be relative to root, it is
     * strongly suggested to set it for each class
     * @return
     */
    @Override
    String getPath() {
        "/emails"
    }

    @Override
    List<Service> getServices() {
        return [
                new Service(
                        path   : "",
                        action : getEmails
                ),
                new Service(
                        path   : "/new",
                        action : getEmailsNew
                ),
                new Service(
                        path   : "/more",
                        action : getEmailsMore,
                        allow  : LoginServiceExample.isAdmin
                ),
                new Service(
                        method : POST,
                        path   : ".save", //will become: /emails.save
                        action : saveEmails,
                        allow  : LoginServiceExample.canEditEmails
                ),
                new Service(
                        path   : "/:user/*",
                        contentType: "text/plain",
                        action : {
                            Request request ->
                                def user = request.getPathParam("user") //It can be ":user" or "user"
                                def domain = request.splat()[0]
                                return user + "@" + domain
                        }
                )
        ]
    }
}
