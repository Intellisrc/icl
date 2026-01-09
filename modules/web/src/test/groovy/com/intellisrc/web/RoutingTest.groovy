package com.intellisrc.web

import com.intellisrc.web.service.Service
import com.intellisrc.web.service.routing.ExactMatcher
import com.intellisrc.web.service.routing.GlobMatcher
import com.intellisrc.web.service.routing.OptionalMatcher
import com.intellisrc.web.service.routing.ParamsMatcher
import com.intellisrc.web.service.routing.RegExMatcher
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @since 2026/01/09.
 */
class RoutingTest extends Specification {
    private static void assertMatcher(
        Class matcherType,
        List<String> aliases = [],
        List<String> matches = [],
        List<String> notMatches = [],
        List<String> collides = [],
        List<String> samples = [] //For Regex
    ) {
        if(matches.empty) {
            matches = aliases
        }
        aliases.each {
            String alias ->
                Service service = new Service(path: alias, samplePaths: samples)
                assert service.matcher.class == matcherType

                (matches).each {
                    println "Matches: ${service.path} vs ${it}"
                    assert service.matcher.matches(it)
                }

                notMatches.each {
                    println "NO Matches: ${service.path} vs ${it}"
                    assert !service.matcher.matches(it)
                }

                (aliases + collides).each {
                    println "Collide: ${service.path} vs ${it}"
                    assert service.collides(new Service(path: it))
                }
        }
    }

    @Unroll
    def "PathMatcher classes should work properly"() {
        expect:
            assertMatcher(
                type,
                aliases,
                matches,
                notMatches,
                collides,
                samples
            )

        // collides are extra paths additionally to "aliases"
        where:
            type            | aliases               | matches               | notMatches                                                                   | collides                                               | samples
            ExactMatcher    | ["test","/test"]      | []                    | ["test/", "/test/", "/", "/other", "other"]                                  | []                                                     | []
            OptionalMatcher | ["test/?","/test/?"]  | ["/test/","test/"]    | ["test/one", "test/one/", "/test/one", "/test/one/", "/", "/other", "other"] | []                                                     | []
            ParamsMatcher   | ["test/:param"]       | ["test/1","/test/1"]  | ["test/1/", "/test/1/", "/", "/test/", "test/", "other/1"]                   | ["test/one","/test/two"]                               | []
            ParamsMatcher   | ["test/:param/"]      | ["test/1/","/test/1/"]| ["test/1", "/test/1", "/", "/test/", "test/", "other/1"]                     | ["/test/one/","test/two/"]                             | []
            GlobMatcher     | ["test/*"]            | ["test/1", "test/1/", "test/1/2/", "/test/1", "/test/1/2"] | ["other/", "/", "other/1/"]             | ["/test/one", "test/two/", "test/three", "/test/four/"] | []
            RegExMatcher    | ["~/[0-9]{3}.jpg/"]    | ["111.jpg","/222.jpg"]| ["0000.jpg","/","/other","test.jpg"]                                        | []                                                      | ["123.jpg", "/123.jpg"]
            RegExMatcher    | ["~/(?<code>[0-9]{3}).jpg/"] | ["111.jpg","/222.jpg"]| ["0000.jpg","/","/other","test.jpg"]                                  | []                                                      | ["123.jpg", "/123.jpg"]
    }

    def "Regular Expression advanced testing"() {
        expect:
            assert false : "Not implemented yet"
    }
}
