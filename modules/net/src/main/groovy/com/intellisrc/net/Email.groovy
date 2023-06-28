package com.intellisrc.net

import groovy.transform.CompileStatic

/**
 * @since 10/21/17.
 */
@CompileStatic
class Email {
    @SuppressWarnings('GrFinalVariableAccess')
    final String user
    @SuppressWarnings('GrFinalVariableAccess')
    final String domain

    Email(String email) throws EmailMalformedException {
        if(isValid(email)) {
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
    static boolean isValid(String email) {
        return email.matches(/(?!.*\.\.)(^[^.@][\w_.+-]+@[\w_.+-]+\.[^@\s.]+$)/)
    }
}
