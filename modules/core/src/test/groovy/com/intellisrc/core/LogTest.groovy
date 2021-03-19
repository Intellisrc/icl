package com.intellisrc.core

import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * @since 17/10/23.
 *
 * NOTE: compressing logs is tested in 'etc'
 */
class LogTest extends Specification {
    class LogChanger extends Log {
        static void clearOnDone() {
            onCleanList.clear()
        }
    }

    def setup() {
        Log.initialized = false //Be sure it hasn't been initialized
        Log.directory = SysInfo.getFile("test-log")
        Log.logFileName = "test.log"
        Log.level = Log.Level.VERBOSE
        Log.printLevel = null
        Log.fileLevel = null
        Log.printAlways = false
        Log.colorAlways = false
        Log.colorInvert = false
        Log.enabled = true
        if (Log.logFile.exists()) {
            Log.logFile.delete()
        }
    }
    def cleanup() {
        Log.directory.listFiles().each { it.delete() }
        Log.directory.deleteDir()
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
            println "Log file to be created in: ${Log.logFile.absolutePath}"
        expect:
            Log.w("This is some warning")
            assert Log.logFile.exists()
            assert Log.logFile.text.contains("some warning")
    }
    
    def "Test parameters"() {
        setup:
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
            Log.init() // Enable will be reset
            Log.enabled = false
            Log.e("Some random error")
        expect:
            assert !Log.logFile.exists()
    }
    
    def "Test Domains in Exception"() {
        setup:
            Log.domains = ["invoke", "LogTest"]
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
    
    def "Variable with error"() {
        setup:
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

    def "Print Log and Log to file levels should be respected"() {
        setup:
            Log.level = Log.Level.DEBUG
            Log.printAlways = true
            Log.fileLevel = Log.Level.WARN
            Log.printLevel = Log.Level.DEBUG
        when:
            Log.w("This should be in both: file and on screen")
            Log.i("This should be only on screen")
        then:
            assert Log.logFile.text.contains("both")
            assert ! Log.logFile.text.contains("only")
        cleanup:
            Log.directory.delete()
    }

    def "On initialization fileLevel and printLevel should use Log.level"() {
        setup:
            Log.level = Log.Level.DEBUG
            Log.init()
        expect:
            assert Log.fileLevel == Log.level
            assert Log.printLevel == Log.level
        cleanup:
            Log.directory.delete()
    }

    def "Changing date should create a new log"() {
        setup:
            SysClock.setClockAt("2000-01-01 23:59:55".toDateTime())
        when:
            Log.w("Reaching the end of day")
            String yesterday = Log.logFile.name
            println "Yesterday log: " + yesterday
        then:
            Log.directory.eachFile {
                println " > " + it.name
            }
            assert Log.logFile.exists()
            assert Log.logFile.text.contains("end")
            assert Log.directory.listFiles().size() == 2 //Including last-test.log link
        when:
            SysClock.setClockAt("2000-01-02 00:00:00".toDateTime())
            Log.i("This is the start of the day")
            println "Today log: " + Log.logFile.name
        then:
            Log.directory.eachFile {
                println " > " + it.name
            }
            assert Log.logFile.exists()
            assert ! Log.logFile.text.contains("end")
            assert Log.logFile.text.contains("start")
            assert Log.directory.listFiles().size() == 3 //Including last-test.log link
            assert yesterday != Log.logFile.name
    }

    //@Unroll
    def "Cleaning should remove old logs"() {
        setup:
            Log.directory = SysInfo.getFile(dir + "-test-log")
            Log.logDays = keep
            LocalDateTime now = SysClock.now
            int logsToCreate = create
            boolean done = false
            LogChanger.clearOnDone()
        when:
            (0..logsToCreate - 1).each {
                SysClock.setClockAt(now.minus(it, ChronoUnit.DAYS).clearTime())
                Log.i("This log is for day: %s", SysClock.now.toLocalDate().YMD)
                Log.logFile.setLastModified(SysClock.now.toMillis())
            }
            println "------- Before cleaning -----------"
            Log.directory.eachFile {
                println " > " + it.name + "\t" + LocalDateTime.fromMillis(it.lastModified()).YMDHmsS
            }
            SysClock.setClockAt(now) //Reset time back to today
        then:
            assert Log.directory.listFiles().size() == logsToCreate + 1 // plus 1 link
        when:
            Log.onCleanDone = {
                println "------- After cleaning -----------"
                Log.directory.eachFile {
                    println " > " + it.name
                }
                done = true
            }
            Log.cleanLogs()
            while(!done) { sleep(50) }
        then:
            assert Log.directory.listFiles().find { it.name == "last-" + Log.logFileName }
            assert Log.directory.listFiles().size() == expected + 1
            noExceptionThrown()
        cleanup :
            LogChanger.clearOnDone()
        where:
            dir | keep | create | expected
             1  |  5   |   10   |    5
             2  |  10  |   12   |    10
             3  |  3   |   5    |    3
             4  |  4   |   3    |    3
             5  |  5   |   5    |    5
             6  |  1   |   8    |    1
             7  |  1   |   1    |    1
    }

    //@Unroll
    def "When rotateOtherLogs is false it should not remove other logs, when its true, it should remove them"() {
        setup:
            Log.directory = SysInfo.getFile(dir + "-test-log")
            Log.logDays = keep
            Log.rotateOtherLogs = rotateOthers
            LocalDateTime now = SysClock.now
            int logsToCreate = create
            boolean done = false
            LogChanger.clearOnDone()
        when:
            (0..logsToCreate - 1).each {
                SysClock.setClockAt(now.minus(it, ChronoUnit.DAYS).clearTime())
                Log.i("This log is for day: %s", SysClock.now.toLocalDate().YMD)
                Log.logFile.setLastModified(SysClock.now.toMillis())
                File otherFile = SysInfo.getFile(Log.directory, SysClock.now.toLocalDate().YMD + "-other.log")
                otherFile.text = "Whatever"
                otherFile.setLastModified(SysClock.now.toMillis())
            }
            println "------- Before cleaning [OTHER: ${rotateOthers ? "YES" : "NO"}] -----------"
            Log.directory.eachFile {
                println " > " + it.name + "\t" + LocalDateTime.fromMillis(it.lastModified()).YMDHmsS
            }
            SysClock.setClockAt(now) //Reset time back to today
        then:
            assert Log.directory.listFiles().size() == (logsToCreate * 2) + 1 // plus 1 link
        when:
            Log.onCleanDone = {
                println "------- After cleaning -----------"
                Log.directory.eachFile {
                    println " > " + it.name
                }
                done = true
            }
            Log.cleanLogs()
            while(!done) { sleep(50) }
        then:
            assert Log.directory.listFiles().find { it.name == "last-" + Log.logFileName }
            assert Log.directory.listFiles().size() == expected + 1
            noExceptionThrown()
        cleanup :
            LogChanger.clearOnDone()
        where:
            dir | keep | create | expected | rotateOthers
            1   |  2   |   7    |    9     | false
            2   |  7   |   2    |    4     | false
            3   |  2   |   7    |    4     | true
            4   |  7   |   2    |    4     | true
    }
}