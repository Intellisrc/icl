package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

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
    List<String> getSamples() {
        List<String> samples = []
        samples << path.replaceAll(/:[a-zA-Z0-9]+/, 'param')
        return samples
    }
}