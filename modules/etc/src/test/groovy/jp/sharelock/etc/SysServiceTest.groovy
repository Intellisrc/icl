package jp.sharelock.etc

import jp.sharelock.etc.dummy.SysServiceDummy
import spock.lang.Specification

/**
 * @since 17/10/23.
 */
class SysServiceTest extends Specification {
    def "Create service"() {
        setup:
            Thread.start {
                SysServiceDummy.main("start")
            }
        when:
            sleep(2000)
            def file = new File(SysServiceDummy.service.lockFile)
            assert file.exists()
        then:
            Thread.start {
                SysServiceDummy.main("stop")
            }
            sleep(2000)
        expect:
            assert ! new File(SysServiceDummy.service.lockFile).exists()
    }
    def "Custom method"() {
        setup:
            SysServiceDummy.main("custom")
        expect:
            assert new File(SysServiceDummy.service.lockFile).exists()
        cleanup:
            SysServiceDummy.exit()
    }
}