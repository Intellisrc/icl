package com.intellisrc.core

import spock.lang.Specification

/**
 * @since 17/10/23.
 */
class LogTest extends Specification {
    def "Simple test"() {
        setup:
            Log.logPath = SysInfo.tempDir
            Log.logFile = "test.log"
            println "Log file to be created in: ${Log.logFile.absolutePath}"
        expect:
            Log.w("This is some warning")
            assert Log.logFile.exists()
            assert Log.logFile.text.contains("some warning")
        cleanup:
            Log.logFile.delete()
    }
    def "Test parameters"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.colorInvert = true
            Log.domain = this.class.canonicalName.tokenize('.').subList(0, 2).join('.')
        when:
            Log.v("This is more than you need to know... %s", "SECRET")
            Log.d("I'm %d%% that this is correct.", 80)
            Log.i("Somewhere between %d and %d", 100, 200)
            Log.w("This is a %s", "warning")
            Log.s("Someone is trying to mess with this code!")
            Log.e("This failed 100%")
        then:
            notThrown Exception
    }
    static class DummyTestException extends Exception {
        String message = "This is the message of the dummy exception"
    }
    def "Test Exception"() {
        setup:
            def throwIt = {
                throw new DummyTestException()
            }
        when:
            try {
                throwIt()
            } catch(Exception e) {
                Log.e("This is an exception: ",e)
            }
        then:
            notThrown Exception
    }
    def "Test Level"() {
        when:
            Log.level = Log.Level.ERROR
            Log.v("No printing this..")
            Log.d("No printing this..")
            Log.i("No printing this..")
            Log.w("No printing this..")
            Log.s("No printing this..")
            Log.e("This failed 100%")
        then:
            notThrown Exception
    }
    def "Respecting Level"() {
        when:
            Log.level = Log.Level.DEBUG
            Log.v("No printing this..")
            Log.d("Debug")
            Log.i("Info")
            Log.w("Warning")
        then:
            notThrown Exception
    }
    def "Disable colors"() {
        when:
            Log.color = false
            Log.w("This warning is so pale...")
        then:
        notThrown Exception
    }
    def "Multiple onLog"() {
        setup:
            int incremental = 0
        when:
            Log.onLog = {
                Log.Level level, String message, Log.Info info ->
                    incremental += 7
                    println "Log One"
            } as Log.OnLog
            Log.onLog = {
                Log.Level level, String message, Log.Info info ->
                    incremental += 100
                    println "Log Two"
            } as Log.OnLog
        then:
            Log.d("This is an message")
            assert incremental == 107
    }
    def "Test disable" () {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.logPath = SysInfo.tempDir
            Log.logFile = "test.log"
            if(Log.logFile.exists()) {
                Log.logFile.delete()
            }
            Log.enabled = false
            Log.e("Some random error")
        expect:
            assert !Log.logFile.exists()
    }
}