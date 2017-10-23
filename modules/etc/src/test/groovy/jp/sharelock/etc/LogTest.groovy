package jp.sharelock.etc

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
}