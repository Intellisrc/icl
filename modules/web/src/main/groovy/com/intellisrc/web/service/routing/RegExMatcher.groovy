package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

/**
 * @since 2026/01/09.
 */
@CompileStatic
class RegExMatcher extends PathMatcher {

    @Override
    boolean matches(String other) {
        other = normalize(other)
        return other ==~ path
    }

    @Override
    String normalize(String path) {
        String normal = super.normalize(path)
        if(normal.startsWith("~/")) {
            normal = normal.replaceFirst(/^~\//,'')
            normal = normal.replaceFirst(/\/$/,'')
            normal = "^\\/" + normal + "\$"
        }
        return normal
    }
//String toMatch = srv.strictPath ? path : (pattern.toString().startsWith("/") ? path : path.replaceFirst(/^\//,''))
    /* TODO:
    Matcher matcher = (toMatch =~ pattern)
    if (matcher.find()) {
        found = true
        if (matcher.hasGroup()) {
            Matcher groupMatcher = Pattern.compile("\\(\\?<(\\w+)>").matcher(pattern.toString())
            while (groupMatcher.find()) {
                String groupName = groupMatcher.group(1)
                if (groupName) {
                    params[groupName] = matcher.group(groupName).toString()
                }
            }
        }
    }*/
}