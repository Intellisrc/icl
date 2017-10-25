package jp.sharelock.net

import groovy.transform.CompileStatic

/**
 * @since 10/21/17.
 */
@CompileStatic
class Email {
    private final String user
    private final String domain

    Email(String email) throws EmailMalformedException {
        if(email.matches(/(?!.*\.\.)(^[^\.@][\w_\.+-]+@[\w_\.+-]+\.[^@\s\.]+$)/)) {
            def parts = email.split("@")
            user = parts[0]
            domain = parts[1]
        } else {
            throw new EmailMalformedException()
        }
    }
    String toString() {
        return user+"@"+domain
    }
    static class EmailMalformedException extends Exception {}
}
