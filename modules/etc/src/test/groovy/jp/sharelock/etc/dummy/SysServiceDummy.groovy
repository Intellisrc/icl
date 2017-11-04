package jp.sharelock.etc.dummy

import groovy.transform.CompileStatic
import jp.sharelock.etc.SysService

/**
 * @since 17/10/23.
 */
@CompileStatic
class SysServiceDummy extends SysService {
    static {
        service = new SysServiceDummy()
        service.lockFile = "dummy.lock" //Optional: see SysService for explanation
    }

    void onInit() {
        println "Initializing"
    }

    @Override
    void onStart() {
        println "Started"
    }

    void onCustom() {
        println "Custom"
    }
}
