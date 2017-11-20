package jp.sharelock.web.samples

/**
 * @since 17/04/19.
 */
import jp.sharelock.web.ServicePath
import jp.sharelock.web.ServicePath.Action
import jp.sharelock.web.ServiciableMultiple

import static jp.sharelock.web.ServicePath.Method.*
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
    List<ServicePath> getServices() {
        return [
                new ServicePath(
                        path   : "",
                        action : getEmails
                ),
                new ServicePath(
                        path   : "/new",
                        action : getEmailsNew
                ),
                new ServicePath(
                        path   : "/more",
                        action : getEmailsMore,
                        allow  : LoginServiceExample.isAdmin
                ),
                new ServicePath(
                        method : POST,
                        path   : ".save", //will become: /emails.save
                        action : saveEmails,
                        allow  : LoginServiceExample.canEditEmails
                )
        ]
    }
}