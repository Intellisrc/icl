package com.intellisrc.etc

import spock.lang.Specification

/**
 * @since 17/10/23.
 */
class SysServiceTest extends Specification {
    def "Create service"() {
        setup:
            Thread.start {
                com.intellisrc.etc.dummy.SysServiceDummy.main("start")
            }
        when:
            sleep(2000)
            def file = new File(com.intellisrc.etc.dummy.SysServiceDummy.service.lockFile)
            assert file.exists()
        then:
            Thread.start {
                com.intellisrc.etc.dummy.SysServiceDummy.main("stop")
            }
            sleep(2000)
        expect:
            assert ! new File(com.intellisrc.etc.dummy.SysServiceDummy.service.lockFile).exists()
    }
    def "Custom method"() {
        setup:
            com.intellisrc.etc.dummy.SysServiceDummy.main("custom")
        expect:
            assert new File(com.intellisrc.etc.dummy.SysServiceDummy.service.lockFile).exists()
        cleanup:
            com.intellisrc.etc.dummy.SysServiceDummy.exit()
    }
}