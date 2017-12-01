package jp.sharelock.web.samples
/**
 * @since 17/04/19.
 */
import jp.sharelock.web.Service
import jp.sharelock.web.Service.Action
import jp.sharelock.web.ServiciableMultiple

import static Service.Method.*
import spark.Request

@groovy.transform.CompileStatic
/**
 * @since 17/04/03.
 */
class EmailService implements ServiciableMultiple {

    Action getEmails = {
        Request request ->
            return [
                    "lepe@sharelock.jp",
                    "lepe@support.ne.jp",
                    "webmaster@support.ne.jp"
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
                    "new_lepe@sharelock.jp"
            ]
    } as Action

    Action getEmailsMore = {
        Request request ->
            println "We are here to see and hear..."
            return [
                    "more_lepe@sharelock.jp"
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
                )
        ]
    }
}