package com.intellisrc.core

import spock.lang.Specification

/**
 * Testing Log Compression
 * Log Zip is tested here because of package dependency
 * @since 19/07/10.
 */
class LogTest extends Specification {
    def "Compressing logs"() {
        when:
            Log.level = Log.Level.VERBOSE
            Log.directory = new File(SysInfo.tempDir)
            Log.logFileName = "test.log"
            if(Log.logFile.exists()) {
                Log.logFile.delete()
            }
            Log.e("Some random error")
            Log.cleanLogs() //should not return error
        then:
            assert Log.compressLog()
        when:
            def gzLog = new File(Log.logFile.absolutePath + ".gz")
        then:
            assert gzLog.exists()
            assert !Log.logFile.exists()
        cleanup:
            if(Log.logFile.exists()) {
                Log.logFile.delete()
            }
    }
}