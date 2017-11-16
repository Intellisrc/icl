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
            Log.w("This is a %s", "warning")
            Log.i("Somewhere between %d and %d", 100, 200)
            Log.d("I'm %d%% that this is correct.", 80)
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
}