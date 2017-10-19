package jp.sharelock.etc

import jp.sharelock.etc.SysInfo
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