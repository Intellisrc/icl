package com.intellisrc.web.samples
/**
 * @since 17/04/19.
 */
import com.intellisrc.web.Service
import com.intellisrc.web.Service.Action
import com.intellisrc.web.ServiciableMultiple

import static com.intellisrc.web.Service.Method.*
import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/03.
 */
class EmailService implements ServiciableMultiple {

    Action getEmails = {
        Request request ->
            return [
                    "admin@example.com",
                    "info@example.com",
                    "webmaster@example.com"
            ]
    } as Action

    Action saveEmails = {
        Request request ->
            return [
                    y : true
            ]
    } as Action

    Action getEmailsNew = {
        Request request ->
            return [
                    "new@example.com"
            ]
    } as Action

    Action getEmailsMore = {
        Request request ->
            println "We are here to see and hear..."
            return [
                    "new@example.com"
            ]
    } as Action

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
                                def user = request.params("user") //It can be ":user" or "user"
                                def domain = request.splat()[0]
                                return user + "@" + domain
                        }
                )
        ]
    }
}
