package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

/**
 * This class provides matching tests for services
 * It is used to improve and simplify the old WebService.matchURI
 * @since 2026/01/09.
 */
@CompileStatic
abstract class PathMatcher {
    protected String path = ""
    /**
     * Returns a finite set of representative paths
     * that this matcher would accept.
     */
    List<String> samples = []

    abstract boolean matches(String path)

    void setPath(String path) {
        this.path = path
        samples << normalize(path)
    }

    String getPath() {
        return normalize(path)
    }

    boolean isPathEmpty() {
        return this.path.empty
    }

    /**
     * True if there exists at least one path
     * matched by both matchers.
     */
    boolean overlaps(PathMatcher other) {
        return samples.any {
            other.matches(it)
        } || other.samples.any {
            matches(it)
        }
    }

    /**
     * Normalizes a path:
     *  - prefix paths with a / unless they start with ~
     * @param path
     * @return
     */
    String normalize(String path) {
        if (!path.startsWith("/") && !path.startsWith("~")) {
            path = "/" + path
        }
        // Replace any double slash in the path:
        return path.replaceAll(/\/\//,'/')
    }
}
