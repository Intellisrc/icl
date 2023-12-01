package com.intellisrc.core.dummy

import com.intellisrc.core.SysService
import groovy.transform.CompileStatic

/**
 * @since 17/10/23.
 */
@CompileStatic
class SysServiceDummy extends SysService {
    static boolean exitCalled = false
    static boolean customCalled = false
    static {
        service = new SysServiceDummy()
        service.lockFile = "dummy.lock" //Optional: see SysService for explanation
    }

    void onInit() {
        println "Command called. Id"
    }

    @Override
    void onStart() {
        println "Started"
    }

    @SuppressWarnings('GrMethodMayBeStatic')
    void onCustom() {
        println "Custom"
        customCalled = true
    }

    @Override
    void onStop() {
        println "Stopping..."
    }

    // For testing...
    @Override
    void kill(int code) {
        println "Exit called with code: $code"
        exitCalled = true
    }
}
