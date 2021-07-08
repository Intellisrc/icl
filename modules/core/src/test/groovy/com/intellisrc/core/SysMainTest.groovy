package com.intellisrc.core

import com.intellisrc.core.dummy.SysMainDummy
import spock.lang.Specification

/**
 * @since 2021/07/07.
 */
class SysMainTest extends Specification {
    def "Must execute onStart without static block"() {
        setup:
            Log.enabled = false
            Config.system.set("main.class","com.intellisrc.core.dummy.SysMainDummy")
            SysMain.exitJava = false
            SysMainDummy.main([] as String[])

        expect:
            assert SysMainDummy.exitCode == 0
            assert SysMainDummy.executed
    }
}
