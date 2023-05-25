package com.intellisrc.web.service

import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpSession

/**
 * HTTPSession wrapper
 * @since 2023/05/25.
 */
@CompileStatic
class Session {
    final HttpSession session

    Session(HttpSession session) {
        this.session = session
    }

    String getId() {
        return session.id
    }

    Object attribute(String key) {
        return session.getAttribute(key)
    }

    void attribute(String key, Object val) {
        if(val == null) {
            removeAttribute(key)
        } else {
            session.setAttribute(key, val)
        }
    }

    boolean hasAttribute(String key) {
        return attributes.contains(key)
    }

    void removeAttribute(String key) {
        session.removeAttribute(key)
    }

    Set<String> getAttributes() {
        return session.attributeNames.toSet()
    }

    Map<String, Object> getMap() {
        return attributes.collectEntries { [(it) : attribute(it) ]}
    }

    void invalidate() {
        session.invalidate()
    }

    boolean isNew() {
        return session.new
    }
}