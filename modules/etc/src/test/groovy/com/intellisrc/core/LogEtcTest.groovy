package com.intellisrc.core

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Testing Log Compression
 * Log Zip is tested here because of package dependency
 * @since 19/07/10.
 */
class LogEtcTest extends Specification {
    class LogChanger extends Log {
        static void clearOnDone() {
            onCleanList.clear()
        }
    }

    def setup() {
        Log.initialized = false //Be sure it hasn't been initialized
        Log.directory = Files.createTempDirectory("log.test").toFile()
        Log.logFileName = "system.log"
        Log.level = Log.Level.VERBOSE
        Log.printLevel = null
        Log.fileLevel = null
        Log.printAlways = false
        Log.colorAlways = false
        Log.colorInvert = false
        Log.enabled = true
    }
    def cleanup() {
        Log.directory.listFiles().each { it.delete() }
        Log.directory.deleteDir()
    }

    def "Compressing logs"() {
        when:
            Log.e("Some random error")
            Log.cleanLogs() //should not return error
        then:
            assert Log.compressLog(Log.logFile)
        when:
            def gzLog = new File(Log.logFile.absolutePath + ".gz")
        then:
            assert gzLog.exists()
            assert !Log.logFile.exists()
    }
    @Unroll
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
            assert Log.directory.listFiles("*.gz").size() == logsToCreate - 1
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
            assert Log.directory.listFiles("*.gz").size() == [keep,create].min()
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
            7  |  2   |   2    |    2
    }

    //@Unroll
    def "When rotateOtherLogs is false it should not remove or compress other logs, when its true, it should remove and compress them"() {
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
            assert Log.directory.listFiles("*.gz").size() == logsToCreate - 1
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
            assert Log.directory.listFiles("*.gz").size() == (rotateOthers ? ([keep, create].min() * 2) - 1 : [keep,create].min())
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