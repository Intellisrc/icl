package com.intellisrc.core

import spock.lang.Specification
import spock.lang.Unroll


/**
 * @since 19/07/10.
 */
class VersionTest extends Specification {
    def "Test Version"() {
        setup:
            String ver = Version.get()
            println ver
        expect:
            assert ver == "0.0" //This will return 0.0 as File.userDir points to the module when running with IDE.
    }
    @Unroll
    def "Parse version"() {
        setup:
            Version version = new Version(full)
            Version version2 = Version.parse(full)

        expect:
            assert version.mayor == mayor
            assert version.minor == minor
            assert version.build == build
            assert version.revision == revision
            assert version.suffix == suffix
            assert version.toString() == full
            assert version.toString() == version2.toString()

        where:
            full              | mayor | minor | build | revision | suffix
            "1"               | 1     |  0    |  0    | 0        | ""
            "1.0"             | 1     |  0    |  0    | 0        | ""
            "1.1.1"           | 1     |  1    |  1    | 0        | ""
            "1.2.3.4"         | 1     |  2    |  3    | 4        | ""
            "1.0b"            | 1     |  0    |  0    | 0        | "b"
            "1.0-beta"        | 1     |  0    |  0    | 0        | "-beta"
            "1.4-beta4"       | 1     |  4    |  0    | 0        | "-beta4"
            "1.2.5-aa"        | 1     |  2    |  5    | 0        | "-aa"
            "1.3.6.4-a1"      | 1     |  3    |  6    | 4        | "-a1"
            "20100101"        | 20100101 |  0 |  0    | 0        | ""
    }
}