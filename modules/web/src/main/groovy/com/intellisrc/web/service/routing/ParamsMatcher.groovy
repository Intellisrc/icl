package com.intellisrc.web.service.routing

import com.intellisrc.core.Log
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
 * However, /some/:param/? may be used as well
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

        // Allow optional matcher:
        String srvPath = path
        if(srvPath.endsWith('/?')) {
            OptionalMatcher om = new OptionalMatcher(path : path)
            srvPath = om.normalized()
        }

        List<String> pathSegs = srvPath.tokenize("/")
        List<String> uriSegs  = uri.tokenize("/")

        if (pathSegs.size() != uriSegs.size()) {
            Log.w("Path sections don't match target sections: (%s) vs (%s)", pathSegs.join(","), uriSegs.join(","))
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