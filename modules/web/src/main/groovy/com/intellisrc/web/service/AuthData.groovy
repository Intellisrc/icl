package com.intellisrc.web.service

import groovy.transform.CompileStatic

/**
 * This class was added to make it clear what kind of
 * data is expected after a successful login using
 * ServiciableAuth
 *
 * @since 2024/04/05.
 */
@CompileStatic
class AuthData {
    Map toStoreInServer = [:]
    Map toSendToClient = [:]

    static AuthData getEmpty() {
        return new AuthData()
    }

    boolean isEmpty() {
        return toStoreInServer.isEmpty()
    }
}
