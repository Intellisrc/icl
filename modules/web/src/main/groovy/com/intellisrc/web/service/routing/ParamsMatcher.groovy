package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Matcher for parametrized paths:
 *
 * /some/:param/:here/
 *
 * NOTE: This will not add trailing slash by default, so:
 *
 * /some/:param is not the same as: /some/:param/ , for example:
 *
 * /img/logo.jpg  vs  /img/uploads/
 *
 * @since 2026/01/09.
 */
@CompileStatic
class ParamsMatcher extends PathMatcher {

    @Override
    boolean matches(String other) {
        other = normalize(other)
        return other ==~ '^' + path.replaceAll(/:[a-zA-Z0-9]+/, '[^/]+') + '$'
    }

    @Override
    Set<String> getSamples() {
        Set<String> samples = []
        samples << path.replaceAll(/:[a-zA-Z0-9]+/, 'param')
        return samples
    }

    @Override
    Map<String, String> getGroups(String uri) {
        uri = normalize(uri)

        List<String> pathSegs = path.tokenize("/")
        List<String> uriSegs  = uri.tokenize("/")

        if (pathSegs.size() != uriSegs.size()) {
            return [:]
        }

        Map<String, String> groups = [:]

        for (int i = 0; i < pathSegs.size(); i++) {
            String p = pathSegs[i]
            String u = uriSegs[i]

            if (p.startsWith(":")) {
                groups[p.substring(1)] = u
            } else if (p != u) {
                return [:]
            }
        }

        return groups
    }
}