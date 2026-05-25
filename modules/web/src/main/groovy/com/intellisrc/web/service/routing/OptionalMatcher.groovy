package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

/**
 * For paths which ends with "/?"
 * Paths will be converted to end with '/', for example:
 *
 * /some/? -> /some/
 * /some   -> /some/
 * /some/  -> /some/ (unchanged)
 *
 * @since 2026/01/09.
 */
@CompileStatic
class OptionalMatcher extends PathMatcher {
    @Override
    boolean matches(String other) {
        other = normalize(other)
        return path == other
    }

    String normalized() {
        return normalize(path)
    }

    @Override
    String normalize(String path) {
        String normal = super.normalize(path)
        if(normal.endsWith("?")) { normal = normal.substring(0, normal.length() - 1) }
        if(! normal.endsWith("/")) { normal = normal + "/" }
        return normal
    }
}