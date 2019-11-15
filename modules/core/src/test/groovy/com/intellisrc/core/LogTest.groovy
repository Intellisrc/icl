package com.intellisrc.core

import spock.lang.Specification

/**
 * @since 17/10/23.
 *
 * NOTE: compressing logs is tested in 'etc'
 */
class LogTest extends Specification {
    def setup() {
        Log.initialized = false //Be sure it hasn't been initialized
        Log.directory = null
        Log.logFileName = ""
        Log.level = Log.Level.VERBOSE
        Log.printAlways = false
        Log.colorAlways = false
        Log.colorInvert = false
        Log.enabled = true
    }
    def cleanup() {
        if(Log.logFile?.exists()) {
            Log.logFile.delete()
        }
    }
    /**
     * Check if link to last log is created successfully
     * The link should be created during initialization and even before printing any log (so the log file may not exists yet)
     * After printing the first message, it will be linked and then created.
     * @return
     *
     */
    def "Simple test"() {
        setup:
            Log.directory = new File(SysInfo.tempDir)
            Log.logFileName = "test.log"
            println "Log file to be created in: ${Log.logFile.absolutePath}"
        expect:
            Log.w("This is some warning")
            assert Log.logFile.exists()
            assert Log.logFile.text.contains("some warning")
    }
    
    def "Test parameters"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.colorInvert = true
            Log.domains << this.class.canonicalName.tokenize('.').subList(0, 2).join('.')
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
    /**
     * In this test, only important parts of the error should be printed (Verbose is ignored)
     */
    def "Test Exception"() {
        setup:
            Log.level = Log.Level.ERROR
            Log.colorInvert = true
            Log.domains << this.class.canonicalName.tokenize('.').subList(0, 2).join('.')
            def throwIt = {
                throw new DummyTestException()
            }
        when:
            Log.i("This should not be printed")
            try {
                throwIt()
            } catch (Exception e) {
                Log.e("This is an exception: ", e)
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
                    println "[Multiple onLog]: Log One"
            } as Log.OnLog
            Log.onLog = {
                Log.Level level, String message, Log.Info info ->
                    incremental += 100
                    println "[Multiple onLog]: Log Two"
            } as Log.OnLog
        then:
            Log.d("This is an message")
            assert incremental == 107
    }
    
    def "Test disable"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.directory = new File(SysInfo.tempDir)
            Log.logFileName = "test.log"
            if (Log.logFile.exists()) {
                Log.logFile.delete()
            }
            Log.init() // Enable will be reset
            Log.enabled = false
            Log.e("Some random error")
        expect:
            assert !Log.logFile.exists()
    }
    
    def "Test Domains in Exception"() {
        setup:
            Log.domains = ["invoke", "LogTest"]
            Log.level = Log.Level.VERBOSE
            Log.colorInvert = true
            def throwIt = {
                throw new DummyTestException()
            }
        when:
            try {
                throwIt()
            } catch (Exception e) {
                Log.e("This is an exception: ", e)
            }
        then:
            /*
             * It should highlight some lines
             */
            notThrown Exception
    }
    
    def "Link log"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.directory = new File(SysInfo.tempDir)
            Log.logFileName = "test.log"
            if (Log.logFile.exists()) {
                Log.logFile.delete()
            }
            Log.e("Some random error")
            // We create a File object here, so we can test if it exists. It is not creating the file (it should have been created in the previous line).
            File link = new File(Log.directory, "last-" + Log.logFileName)
        expect:
            assert Log.logFile.exists(): "Log file should have been created"
            assert link.exists(): "Link was not created"
        cleanup:
            if (link?.exists()) {
                link.delete()
            }
    }
    
    def "Unable to create directory"() {
        setup:
            Log.level = Log.Level.VERBOSE
            Log.directory = new File("/dev/null/err")
            Log.logFileName = "test.log"
            Log.printAlways = false
            Log.i("It must not log this.. just display")
        expect:
            assert Log.logFileName.empty : ("Log has name: " + Log.logFileName)
            assert ! Log.directory.exists() : ("Dir existed: " + Log.directory.absolutePath)
            assert Log.logFile == null : ("Log existed: " + Log.logFile?.absolutePath)
            assert Log.printAlways
    }
    
    def "Variable with error"() {
        setup:
            Log.level = Log.Level.VERBOSE
            String initializer = "me"
        when:
            try {
                throw new Exception("Manually triggered")
            } catch(Exception e) {
                Log.e("This error was generated by %s.", initializer, e)
            }
        then:
            notThrown Exception
    }
}