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

    Object attribute(String key) {
        return session.getAttribute(key)
    }

    void attribute(String key, Object val) {
        session.setAttribute(key, val)
    }

    List<String> getAttributes() {
        return session.attributeNames.toList()
    }

    Map<String, Object> getAttributesMap() {
        return attributes.collectEntries { [(it) : attribute(it) ]}
    }

    void invalidate() {
        session.invalidate()
    }
}