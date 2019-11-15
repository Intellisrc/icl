package com.intellisrc.core

import spock.lang.Specification

import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Testing Log Compression
 * Log Zip is tested here because of package dependency
 * @since 19/07/10.
 */
class LogTest extends Specification {
    def "Compressing logs"() {
        setup:
            Log.initialized = false
            Log.level = Log.Level.VERBOSE
            Log.directory = Files.createTempDirectory("log.test").toFile()
            Log.logFileName = "system.log"
        when:
            if(Log.logFile.exists()) {
                Log.logFile.delete()
            }
            Log.e("Some random error")
            Log.cleanLogs() //should not return error
        then:
            assert Log.compressLog(Log.logFile)
        when:
            def gzLog = new File(Log.logFile.absolutePath + ".gz")
        then:
            assert gzLog.exists()
            assert !Log.logFile.exists()
        cleanup:
            if(Log.logFile.exists()) {
                Log.logFile.delete()
            }
            if(gzLog.exists()) {
                gzLog.delete()
            }
    }
    def "Delete old logs and compress the rest"() {
        setup:
            Log.initialized = false
            Log.level = Log.Level.VERBOSE
            Log.directory = Files.createTempDirectory("log.test").toFile()
            Log.logDays = 5
            LocalDateTime now = LocalDateTime.now()
            int logsToCreate = 30
        when:
            (0..logsToCreate).each {
                File log = new File(Log.directory, "test-${it}.log")
                log.text = "Some text in the log"
                log.setLastModified(it == 0 ? now.toMillis()
                                            : now.minusDays(it).toMillis())
            }
        then:
            assert Log.directory.listFiles().size() == logsToCreate + 1
            Log.cleanLogs()
        expect:
            assert Log.directory.listFiles().size() == Log.logDays + 1
            assert new File(Log.directory, "test-0.log").exists()
            assert Log.directory.listFiles({ it.name.endsWith(".gz") } as FileFilter).size() == Log.logDays
        cleanup:
            Log.directory.listFiles().each { it.delete() }
            Log.directory.deleteDir()
    }
    /**
     * When logDays is zero there should not be any file before today.
     * @return
     */
    def "When logDays is zero"() {
        setup:
            Log.initialized = false
            Log.level = Log.Level.VERBOSE
            Log.directory = Files.createTempDirectory("log.test").toFile()
            Log.logDays = 0
            LocalDateTime now = LocalDateTime.now()
            int logsToCreate = 30
        when:
            (0..logsToCreate).each {
                File log = new File(Log.directory, "test-${it}.log")
                log.text = "Some text in the log"
                log.setLastModified(it == 0 ? now.toMillis()
                        : now.minusDays(it).toMillis())
            }
        then:
            assert Log.directory.listFiles().size() == logsToCreate + 1
            Log.cleanLogs()
        expect:
            assert Log.directory.listFiles().size() == Log.logDays + 1
            assert new File(Log.directory, "test-0.log").exists()
            assert Log.directory.listFiles({ it.name.endsWith(".gz") } as FileFilter).size() == Log.logDays
        cleanup:
            Log.directory.listFiles().each { it.delete() }
            Log.directory.deleteDir()
    }
}