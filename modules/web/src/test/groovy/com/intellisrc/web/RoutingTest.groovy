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
        List<String> aliases    = [],
        List<String> matches    = [],
        List<String> notMatches = [],
        List<String> collides   = [],
        List<String> samples    = [] //For Regex
    ) {
        if(matches.empty) {
            matches = aliases
        }
        aliases.each {
            String alias ->
                Service service = new Service(path: alias, samplePaths: samples)
                assert service.matcher.class == matcherType

                if(matcherType != RegExMatcher) {
                    assert service.matcher.samples.size() > 0
                }

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

    private static void assertRegexCollisionCheck(
        String path,
        List<String> samples            = [],
        String collidePath,
        List<String> collideSamples     = [],
        String noCollidePath,
        List<String> noCollideSamples   = []
    ) {
        Service service = new Service(path: path, samplePaths: samples)
        Service collide = new Service(path: collidePath, samplePaths: collideSamples)
        Service noCollide = new Service(path: noCollidePath, samplePaths: noCollideSamples)

        assert service.matcher.class == RegExMatcher
        assert collide.matcher.class == RegExMatcher
        assert noCollide.matcher.class == RegExMatcher

        assert service.collides(collide) : "Should collide with collide service"
        assert ! service.collides(noCollide) : "Should not collide with noCollide service"
    }

    private static void assertRegexMatcher(String regex, List<String> match, List<String> noMatch) {
        println "----------------[ $regex ]------------------------"
        Service service = new Service(path: regex, samplePaths: [])

        assert service.matcher.class == RegExMatcher
        match.each {
            println "Match : ${it}"
            assert service.matcher.matches(it)
        }
        noMatch.each {
            println "NO Match : ${it}"
            assert ! service.matcher.matches(it)
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
            RegExMatcher    | ["~/[0-9]{3}.jpg/"]    | ["111.jpg","/222.jpg"]| ["0000.jpg","/","/other","test.jpg"]                                        | ["/:other"]                                                      | ["123.jpg", "/123.jpg"]
            RegExMatcher    | ["~/test/(?<code>[0-9]{3}).jpg/"] | ["test/111.jpg","/test/222.jpg"]| ["/test/0000.jpg","/","/other","test.jpg","/test/","test/0000.jpg"] | ["/test/:other"]                      | ["/test/123.jpg"]
    }

    @Unroll
    def "Regular Expression collision test"() {
        expect:
            assertRegexCollisionCheck(regex, regexSamples, conflict, conflictSamples, noConflict, noConflicSamples)

        where:
            regex                           | regexSamples                      | conflict             | conflictSamples                | noConflict                 | noConflicSamples
            /[0-9]{4}(-[a-z]{6})?\.html/    | ["2000-abcdef.html", "2001.html"] | /\w{4,6}\..+/        | ["abc123.jpg","a12345.mp3"]    | /[0-9]{6}\.html/           | ["111222.html"]
            /^[0-9]{4}(-[a-z]{6})?\.html$/    | ["2000-abcdef.html", "2001.html"] | /\w{4,6}\..+/      | ["abc123.jpg","a12345.mp3"]    | /[0-9]{6}\.html/           | ["111222.html"]
    }

    /**
     * Full paths are: /^...$/
     */
    @Unroll
    def "Strict regex should match full paths and vice-versa"() {
        expect:
            assertRegexMatcher(regex, matches, noMatches)

        where:
            regex                           | matches                           | noMatches
            /[0-9]{4}(-[a-z]{6})?\.html/    | ["2000-abcdef.html", "2001.html"] | ["111222.html", "2000-aa.html"]
            /.*[0-9]{4}(-[a-z]{6})?\.html/  | ["2000-abcdef.html", "2001.html", "20-2000-abcdef.html","111222.html","/test/2000-abcdef.html"] | ["2000-aa.html","200.html","200-abcdef.html"]
        // ^ and $ are optional as they are always added automatically
            /^[0-9]{4}(-[a-z]{6})?\.html$/  | ["2000-abcdef.html", "2001.html", "/2000-abcdef.html", "/2001.html"] | ["111222.html", "2000-aa.html","102000-abcdef.html"]
        // slash "/" at the beginning of a regex should be removed as it is not required:
            /^\/[0-9]{4}(-[a-z]{6})?\.html$/  | ["2000-abcdef.html", "2001.html", "/2000-abcdef.html", "/2001.html"] | ["111222.html", "2000-aa.html","102000-abcdef.html"]
            /^\/[0-9]{4}\/$/                  | ["2000/", "/2001/"] | ["111222/", "/2000-aa/","/102000/","/2001","2001"]
    }

    def "Regex groups should be captured"() {
        setup:
            Service service = new Service(path: "(?<year>[0-9]{4})-(?<month>[0-9]{2})-(?<day>[0-9]{2}).html", samplePaths: ["2000-01-30"])
            assert service.matcher.class == RegExMatcher
            service.matcher.samples.each {
                println it
            }
            assert service.matcher.samples.size() == 1 : "samplePaths should be added to matcher samples"

        when:
            Map groups = service.matcher.getGroups("/2000-12-31.html")
        then:
            assert ! groups.isEmpty()
            assert groups.year == "2000"
            assert groups.month == "12"
            assert groups.day == "31"
    }

    def "Params groups should be captured"() {
        setup:
            Service service = new Service(path: "/test/:user/:area/")
            assert service.matcher.class == ParamsMatcher
            service.matcher.samples.each {
                println it
            }
            assert service.matcher.samples.size() == 1 : "samples should be generated"

        when:
            Map groups = service.matcher.getGroups("/test/peter/sports/")
        then:
            assert ! groups.isEmpty()
            assert groups.user == "peter"
            assert groups.area == "sports"
    }

    def "Glob group should be captured correctly"() {
        setup:
            Service service = new Service(path: "/test/*")
            assert service.matcher.class == GlobMatcher
            assert service.matcher.samples.size() > 1 : "samples should be generated"

            service.matcher.samples.each {
                println it
            }

        when:
            Map groups = service.matcher.getGroups("/test/peter/sports/")
        then:
            assert ! groups.isEmpty()
            assert groups.glob == "peter/sports/"

    }
}