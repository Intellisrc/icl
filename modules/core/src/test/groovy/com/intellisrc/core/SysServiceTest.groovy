package com.intellisrc.core

import com.intellisrc.core.dummy.SysServiceDummy
import spock.lang.Specification

/**
 * @since 17/10/23.
 */
class SysServiceTest extends Specification {
    def cleanup() {
        //As it is static, we need to reset it in consecutive tests
        if(SysServiceDummy.service) {
            SysServiceDummy.service = new SysServiceDummy()
        }
    }
    def "Create service"() {
        setup:
            Thread.start {
                SysServiceDummy.main("start")
            }
            sleep(Millis.SECOND)
            def file = new File(SysServiceDummy.service.lockFile)
        expect:
            assert file.exists()
        when:
            Thread.start {
                SysServiceDummy.main("stop")
            }
            sleep(Millis.SECOND)
        then:
            assert SysServiceDummy.exitCalled
            assert ! new File(SysServiceDummy.service.lockFile).exists()
    }
    def "Custom method"() {
        setup:
            SysServiceDummy.main("custom")
            sleep(Millis.SECOND)
        expect:
            assert ! new File(SysServiceDummy.service.lockFile).exists()
            assert SysServiceDummy.customCalled
        cleanup:
            SysServiceDummy.service.exit()
    }
}
