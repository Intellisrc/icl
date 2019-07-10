package com.intellisrc.core

import spock.lang.Specification


/**
 * @since 19/07/10.
 */
class VersionTest extends Specification {
    def "Test Version"() {
        setup:
            String ver = Version.get()
            println ver
        expect:
            assert ver == "0.0" //This will return 0.0 as SysInfo.userDir points to the module when running with IDE.
    }
}