package com.intellisrc.core

import spock.lang.Specification

/**
 * @since 17/10/23.
 */
class LogTest extends Specification {
    def "Simple test"() {
        setup:
            Log.logFile = "test.log"
            Log.w("This is some warning")
        expect:
            def file = Log.logPath + Log.logFile
            def oFile = new File(file)
            assert oFile.exists()
            println "Log file created in: $file"
            assert oFile.text.contains("some warning")
            println "Content:"
            println oFile.text
        cleanup:
            oFile.delete()
    }
    def "Specifying path"() {
        setup:
        // If can also be specified at the config.properties file like: log.file, log.path
            Log.logFile = "test.log"
            Log.logPath = "/tmp/"
            Log.w("This is some warning")
        expect:
            def file = Log.logPath + Log.logFile
            def oFile = new File(file)
            assert oFile.exists()
            println "Log file created in: $file"
            assert oFile.text.contains("some warning")
            println "Content:"
            println oFile.text
        cleanup:
            oFile.delete()
    }
    def "Test parameters"() {
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
            Log.logPath = "/tmp/"
            Log.logFile = "test.log"
            Log.enabled = false
            Log.e("Some random error")
        expect:
            assert !Log.logFile.exists()
    }
}