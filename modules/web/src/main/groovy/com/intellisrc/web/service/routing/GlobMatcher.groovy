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
    Set<String> getSamples() {
        String base = normalizedGlob
        Set<String> samples = []
        (1..10).each {
            base += it + "/"
            samples << base
            samples << (base + "file.test")
        }
        return samples
    }

    // Remove "*" for comparison
    private String getNormalizedGlob() {
        return path.replace("*", "")
    }

    private String normalizeRequest(String path) {
        String normal = super.normalize(path)
        if(! path.endsWith("/")) { normal = normal + "/" }
        return normal
    }

    @Override
    Map<String, String> getGroups(String uri) {
        uri = normalizeRequest(uri)
        String base = normalizedGlob

        if (! uri.startsWith(base)) {
            return [:]
        }

        return [ glob: uri.substring(base.length()) ]
    }
}