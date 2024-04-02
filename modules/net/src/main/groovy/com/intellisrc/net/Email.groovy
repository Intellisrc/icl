package com.intellisrc.net

import groovy.transform.CompileStatic

/**
 * This class will take a string and wrap it to ensure a
 * valid* email is used. Be aware that the email validation
 * here only covers a wide range of uses but not all of them,
 * specially emails with quotation or usual domain annotations.
 * It supports internationalization (special characters)
 *
 * @since 10/21/17.
 */
@CompileStatic
class Email {
    static String validRegex = /(?!.*(^\.|\.\.|\.@|@\.|@.*@|@-|\s|[(),:;<>\[\\\]]+))(^.+@.+\.[^@\s.]+$)/
    static String validRegexStrict = /(?!.*(^\.|\.\.|\.@|@\.|@.*@|@-|\s|[(),:;<>\[\\\]]+))(^[\w_.+-]+@[\w_.-]+\.[^@\s.]+$)/
    @SuppressWarnings('GrFinalVariableAccess')
    final String user
    @SuppressWarnings('GrFinalVariableAccess')
    final String domain
    /**
     * Wraps an string as an Email. It throws 'EmailMalformedException' if the email is incorrect
     * @param email     : String containing an email
     * @param strict    : If true, will not support emails with special characters like internationalization
     * @throws EmailMalformedException
     */
    Email(String email, boolean strict = false) throws EmailMalformedException {
        if(isValid(email, strict)) {
            def parts = email.split("@")
            user = parts[0]
            domain = parts[1]
        } else {
            throw new EmailMalformedException()
        }
    }
    @Override
    String toString() {
        return user+"@"+domain
    }

    static class EmailMalformedException extends Exception {}
    /**
     * When used as boolean... check if email is correct and not empty
     * @return
     */
    boolean toBoolean() {
        return !(user.isEmpty() || user.isEmpty())
    }
    /**
     * Returns true if its a valid email address
     * @param email
     * @return
     */
    static boolean isValid(String email, boolean strict = false) {
        return email.matches(strict ? validRegexStrict : validRegex)
    }
}
