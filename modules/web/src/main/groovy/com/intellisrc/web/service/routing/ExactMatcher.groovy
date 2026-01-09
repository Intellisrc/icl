package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

/**
 * Only exact match
 * @since 2026/01/09.
 */
@CompileStatic
class ExactMatcher extends PathMatcher {
    @Override
    boolean matches(String other) {
        other = normalize(other)
        return path == other
    }
}