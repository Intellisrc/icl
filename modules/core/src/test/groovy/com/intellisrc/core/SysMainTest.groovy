package com.intellisrc.core

import com.intellisrc.core.dummy.SysMainDummy
import spock.lang.Specification

/**
 * @since 2021/07/07.
 */
class SysMainTest extends Specification {
    def "Must execute onStart without static block"() {
        setup:
            Config.system.set("main.class", this.class.packageName + ".dummy.SysMainDummy")
            SysMain.exitJava = false
            SysMainDummy.main([] as String[])

        expect:
            assert SysMainDummy.exitCode == 0
            assert SysMainDummy.executed
    }
}
