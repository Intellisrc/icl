package com.intellisrc.core

import spock.lang.Specification

/**
 * Created by lepe on 17/02/23.
 */
class SysInfoTest extends Specification {
    def "OS detection"() {
        expect:
            SysInfo.getOS() != SysInfo.OS.UNKNOWN
    }
}