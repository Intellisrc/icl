package com.intellisrc.web.service.routing

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

/**
 * This class is used when params are mixed with glob match, for example:
 *
 * /users/:id/*
 *
 * @since 2026/01/29.
 */
@CompileStatic
class GlobParamsMatcher extends PathMatcher {

	@Override
	boolean matches(String other) {
		other = normalize(other)

		List<String> pathSegs = path.tokenize("/")
		List<String> uriSegs  = splitPreserveTrailing(other)

		int starIndex = pathSegs.indexOf("*")

		if (starIndex == -1 || starIndex != pathSegs.size() - 1) {
			return false
		}

		// '*' must match at least something
		if (uriSegs.size() <= starIndex) {
			return false
		}

		for (int i = 0; i < starIndex; i++) {
			String p = pathSegs[i]
			String u = uriSegs[i]

			if (p.startsWith(":")) continue
			if (p != u) return false
		}

		return true
	}

	@Override
	Map<String, String> getGroups(String uri) {
		uri = normalize(uri)

		List<String> pathSegs = path.tokenize("/")
		List<String> uriSegs  = splitPreserveTrailing(uri)

		int starIndex = pathSegs.indexOf("*")
		if (starIndex == -1 || uriSegs.size() <= starIndex) {
			return [:]
		}

		Map<String, String> groups = [:]

		for (int i = 0; i < starIndex; i++) {
			if (pathSegs[i].startsWith(":")) {
				groups[pathSegs[i].substring(1)] = uriSegs[i]
			}
		}

		groups["glob"] = uriSegs
			.subList(starIndex, uriSegs.size())
			.join("/")

		return groups
	}

    @Override
    Set<String> getSamples() {
        Set<String> samples = []

        List<String> pathSegs = path.tokenize("/")
        String base = pathSegs.collect {
            it.startsWith(":") ? "param" : it
        }.findAll { it != "*" }.join("/")

        samples << "/$base/x".toString()
        samples << "/$base/x/y".toString()
        samples << "/$base/file.txt".toString()

        return samples
    }

	protected static List<String> splitPreserveTrailing(String uri) {
		boolean trailingSlash = uri.endsWith("/")

		List<String> segs = uri
			.replaceAll("^/+", "")
			.replaceAll("/+\$", "")
			.tokenize("/")

		if (trailingSlash) {
			segs << ""   // represent trailing slash
		}

		return segs
	}

}

