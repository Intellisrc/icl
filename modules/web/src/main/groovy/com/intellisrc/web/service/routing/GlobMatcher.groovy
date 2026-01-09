package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

/**
 * Glob matcher, for example:
 *
 * /some/*
 *
 * @since 2026/01/09.
 */
@CompileStatic
class GlobMatcher extends PathMatcher {
    @Override
    boolean matches(String other) {
        other = normalizeRequest(other)
        return other.startsWith(normalizedGlob)
    }

    @Override
    List<String> getSamples() {
        String base = normalizedGlob
        List<String> samples = (1..20).collect {
            base += it + "/"
            return base
        }
        return samples
    }

    // Remove "*" for comparison
    String getNormalizedGlob() {
        return path.replace("*", "")
    }

    String normalizeRequest(String path) {
        String normal = super.normalize(path)
        if(! path.endsWith("/")) { normal = normal + "/" }
        return normal
    }
}