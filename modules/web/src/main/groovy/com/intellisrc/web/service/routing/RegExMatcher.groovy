package com.intellisrc.web.service.routing

import groovy.transform.CompileStatic

import java.util.regex.Matcher
import java.util.regex.Pattern

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
            normal = normal.replaceFirst(/^\^/,'')
            normal = normal.replaceFirst(/\$$/,'')
            normal = normal.replaceFirst(/^\//,'')
            normal = "^\\/" + normal + "\$"
        }
        return normal
    }

    @Override
    Map<String, String> getGroups(String uri) {
        Map<String, String> groups = [:]
        if(matches(uri)) {
            Matcher matcher = (uri =~ path)
            if (matcher.find()) {
                if (matcher.hasGroup()) {
                    Matcher groupMatcher = Pattern.compile("\\(\\?<(\\w+)>").matcher(path)
                    while (groupMatcher.find()) {
                        String groupName = groupMatcher.group(1)
                        if (groupName) {
                            groups[groupName] = matcher.group(groupName).toString()
                        }
                    }
                }
            }
        }
        return groups
    }
}