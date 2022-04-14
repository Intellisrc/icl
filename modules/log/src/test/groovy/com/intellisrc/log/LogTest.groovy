package com.intellisrc.log

import com.intellisrc.core.AnsiColor
import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import org.slf4j.event.Level
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDateTime

/**
 * @since 17/10/23.
 *
 * NOTE: compressing logs is tested in 'etc'
 */
class LogTest extends Specification {
    @Shared
    CommonLogger logger
    @Shared
    FileLogger fileLogger
    @Shared
    PrintLogger printLogger

    def setup() {
        logger = CommonLogger.default
        // Do not initialize default
        logger.initialized = true

        BaseLogger basePrinter = new BaseLogger()

        fileLogger = new FileLogger(
            logFileName : "test.log",
            logDir: File.get(File.tempDir, "test-log")
        )
        fileLogger.initialize(basePrinter)
        fileLogger.onCleanList.clear()

        printLogger = new PrintLogger()
        printLogger.initialize(basePrinter)
        printLogger.showPackage = true
        printLogger.level = Level.TRACE

        logger.initialize()
        logger.printers.clear()
        logger.printers << fileLogger
        logger.printers << printLogger
    }

    def cleanup() {
        if(fileLogger.logDir.exists()) {
            fileLogger.logDir.eachFile {
                it.delete()
            }
            fileLogger.logDir.deleteDir()
        }
        logger.domains.clear()
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
            println "Log file to be created in: ${fileLogger.logFile.absolutePath}"
        expect:
            Log.w("This is some warning")
            assert fileLogger.logFile.exists()
            assert fileLogger.logFile.text.contains("some warning")
    }

    def "Test parameters"() {
        setup:
            fileLogger.colorInvert = true
            printLogger.showStackTrace = false
            StringLogger stringLogger = new StringLogger()
            stringLogger.level = Level.TRACE
            logger.addPrinter(stringLogger)
            logger.domains << this.class.canonicalName.tokenize('.').subList(0, 2).join('.')
            SysClock.setClockAt("2021-08-11 14:44:32".toDateTime())
        when:
            Log.i("[%d] This message (%s) has many %.2f parameters: {}", 1, SysClock.now, 10/3d, new File("/tmp/test.txt"))
            assert stringLogger.content.contains("[1] This message (2021-08-11 14:44:32) has many 3.33 parameters") && stringLogger.clear()
            Log.i("Mixing %d arg with {} arg", 1, 2)
            assert stringLogger.content.contains("Mixing 1 arg with 2 arg") && stringLogger.clear()
            Log.w("Other way could be: {}%", 100)
            assert stringLogger.content.contains("Other way could be: 100%") && stringLogger.clear()
            Log.w("What does SLF4J does here? {}", 10/3d)
            assert stringLogger.content.contains("What does SLF4J does here? 3.3333") && stringLogger.clear()
            Log.v("This is more than you need to know... %s", "SECRET")
            assert stringLogger.content.contains("This is more than you need to know... SECRET") && stringLogger.clear()
            Log.t("This is the {}% same as verbose", 100)
            assert stringLogger.content.contains("This is the 100% same as verbose") && stringLogger.clear()
            Log.d("I'm %d%% that this is correct.", 80)
            assert stringLogger.content.contains("I'm 80% that this is correct.") && stringLogger.clear()
            Log.i("Somewhere between {}% and {}%", 100, 200)
            assert stringLogger.content.contains("Somewhere between 100% and 200%") && stringLogger.clear()
            Log.w("This is a %s", "warning")
            assert stringLogger.content.contains("This is a warning") && stringLogger.clear()
            Log.e("This failed 100%")
            assert stringLogger.content.contains("This failed 100%") && stringLogger.clear()
            Log.i("The percentage is 100%, but it could be: %d%%", 99)
            assert stringLogger.content.contains("The percentage is 100%, but it could be: 99%") && stringLogger.clear()
            Log.w("What %s... nooo wait!!!", "???")
            assert stringLogger.content.contains("What ???... nooo wait!!!") && stringLogger.clear()
            Log.e("Upps! too late! its now: %s", SysClock.now)
            assert stringLogger.content.contains("Upps! too late! its now: 2021-08-11 14:44:32") && stringLogger.clear()
        then:
            notThrown Exception
        cleanup:
            stringLogger.close()
    }

    static class DummyTestException extends Exception {
        String message = "This is the message of the dummy exception"
    }
    /**
     * In this test, only important parts of the error should be printed (Verbose is ignored)
     */
    def "Test Exception"() {
        setup:
            fileLogger.level = printLogger.level = Level.ERROR
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
            assert !fileLogger.logFile.text.contains("This should not be printed")
    }

    def "Test Level"() {
        when:
            fileLogger.level = printLogger.level = Level.ERROR
            Log.v("No printing this..")
            Log.d("No printing this..")
            Log.i("No printing this..")
            Log.w("No printing this..")
            Log.e("This failed 100%")
        then:
            notThrown Exception
            assert !fileLogger.logFile.text.contains("No printing this..")
            assert fileLogger.logFile.text.contains("This failed 100%")

    }

    def "Respecting Level"() {
        when:
            fileLogger.level = printLogger.level = Level.DEBUG
            Log.v("No printing this..")
            Log.d("Debug")
            Log.i("Info")
            Log.w("Warning")
        then:
            notThrown Exception
            assert !fileLogger.logFile.text.contains("No printing this..")
            assert fileLogger.logFile.text.contains("Debug")
            assert fileLogger.logFile.text.contains("Info")
            assert fileLogger.logFile.text.contains("Warning")
    }

    def "Disable colors"() {
        when:
            fileLogger.useColor = printLogger.useColor = false
            Log.w("This warning is so pale...")
        then:
            notThrown Exception
            assert !fileLogger.logFile.text.contains(AnsiColor.YELLOW)
            assert fileLogger.logFile.text.contains("warning")
    }

    def "Test disable"() {
        setup:
            fileLogger.enabled = printLogger.enabled = false
            Log.e("Some random error")
        expect:
            assert !fileLogger.logFile.text.contains("Some random")
    }

    def "Test Domains in Exception"() {
        setup:
            logger.domains = ["LogTest"]
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
            assert fileLogger.logFile.text.contains(AnsiColor.YELLOW)
    }

    def "Link log"() {
        setup:
            Log.e("Some random error")
            // We create a File object here, so we can test if it exists. It is not creating the file (it should have been created in the previous line).
            File link = new File(fileLogger.logDir, "last-" + fileLogger.logFileName)
        expect:
            assert fileLogger.logFile.exists(): "Log file should have been created"
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
            } catch (Exception e) {
                Log.e("This error was generated by %s.", initializer, e)
            }
        then:
            notThrown Exception
    }

    def "Changing date should create a new log"() {
        setup:
            LocalDateTime now = SysClock.now
            assert fileLogger.logDir.exists()
        when:
            Log.i("First Log")
            String startLog = fileLogger.logFile.absolutePath
            println "Starting log: " + startLog
        then:
            fileLogger.logDir.eachFile {
                println " > " + it.name
            }
            assert fileLogger.logFile.exists()
            assert fileLogger.logFile.text.contains("First Log")
            assert fileLogger.logDir.listFiles().size() == 2 //Including last-test.log link
        when:
            SysClock.setClockAt(now.plusDays(1))
            Log.i("One day after...")
            println "Next log: " + fileLogger.logFile.name
        then:
            fileLogger.logDir.eachFile {
                println " > " + it.name
            }
            assert fileLogger.logFile.exists()
            assert !fileLogger.logFile.text.contains("First Log")
            assert fileLogger.logFile.text.contains("One day after")
            assert fileLogger.logDir.listFiles().size() == 3 //Including last-test.log link
            assert startLog != fileLogger.logFile.name
    }

    @Unroll
    def "Cleaning should remove old logs"() {
        setup:
            File baseDir = File.get(File.tempDir, "test-many-dir")
            println "Location: " + baseDir.absolutePath
            println "[$base : Create: $create, Keep: $keep = $expected]"
            fileLogger.logDir = File.get(baseDir, base + "-test-log")
            fileLogger.logDays = keep
            fileLogger.compress = compress
            LocalDateTime now = SysClock.now
            int logsToCreate = create
            boolean done = false
        when:
            // The last log will be today (without date)
            SysClock.setClockAt(now.minusDays(logsToCreate + 1))
            (1..logsToCreate).each {
                SysClock.setClockAt(SysClock.now.plusDays(1).clearTime())
                Log.i("[%d] This log is for day: %s", it, SysClock.now.toLocalDate().YMD)
                fileLogger.logFile.setLastModified(SysClock.now.toMillis())
            }
            SysClock.setClockAt(now) //Reset time back to today
            Log.w("This is for today")
            println "------- Before cleaning -----------"
            fileLogger.logDir.eachFile {
                println " > " + it.name + "\t" + LocalDateTime.fromMillis(it.lastModified()).YMDHmsS
            }
        then:
            assert fileLogger.logDir.listFiles().size() == logsToCreate + 2 // plus 1 link
        when:
            fileLogger.onCleanDone = {
                println "------- After cleaning -----------"
                if(fileLogger.logDir.exists()) {
                    fileLogger.logDir.eachFile {
                        println " > " + it.name
                    }
                }
                done = true
            }
            fileLogger.cleanLogs()
            while (!done) {
                sleep(50)
            }
        then:
            assert fileLogger.logDir.listFiles().find { it.name == "last-" + fileLogger.logFileName }
            assert fileLogger.logDir.listFiles().size() == expected + 1
            if(compress) {
                assert fileLogger.logDir.listFiles().findAll { it.name.endsWith(".gz") }.size() == expected - 1
                assert fileLogger.logDir.listFiles().findAll { it.name.endsWith(".log") }.size() == 2
            } else {
                assert fileLogger.logDir.listFiles().findAll { it.name.endsWith(".gz") }.size() == 0
            }
            noExceptionThrown()
        cleanup:
            if(baseDir.exists()) {
                baseDir.deleteDir()
            }
        where:
            base | keep | create | expected | compress
            1    | 5    | 10     | 5        | false
            2    | 10   | 12     | 10       | false
            3    | 3    | 5      | 3        | false
            4    | 5    | 3      | 4        | false     // The reason is because we won't remove any log as we need at least 5 to remove
            5    | 5    | 5      | 5        | false
            6    | 1    | 8      | 1        | false
            7    | 1    | 1      | 1        | false

            8    | 5    | 10     | 5        | true
            9    | 10   | 12     | 10       | true
            10   | 3    | 5      | 3        | true
            11   | 5    | 3      | 4        | true
            12   | 5    | 5      | 5        | true
            13   | 1    | 8      | 1        | true
            14   | 1    | 1      | 1        | true
    }

    @Unroll
    def "When rotateOtherLogs is false it should not remove other logs, when its true, it should remove them"() {
        setup:
            fileLogger.logDir = File.get(File.tempDir, "test-log-" + dir)
            fileLogger.logDays = keep
            fileLogger.rotateOtherLogs = rotateOthers
            LocalDateTime now = SysClock.now
            int logsToCreate = create
            boolean done = false
            Log.i("Keep: %d, Create: %d, Rotate Other? %s", keep, create, rotateOthers ? "Y" : "N")
        when:
            // The last log will be today (without date)
            SysClock.setClockAt(now.minusDays(logsToCreate + 1))
            (1..logsToCreate).each {
                SysClock.setClockAt(SysClock.now.plusDays(1).clearTime())
                Log.i("[%d] This log is for day: %s", it, SysClock.now.toLocalDate().YMD)
                fileLogger.logFile.setLastModified(SysClock.now.toMillis())
                File otherFile = File.get(fileLogger.logDir, SysClock.now.toLocalDate().YMD + "-other.log")
                otherFile.text = "Whatever"
                otherFile.setLastModified(SysClock.now.toMillis())
            }
            SysClock.setClockAt(now) //Reset time back to today
            File otherFile = File.get(fileLogger.logDir, "other.log")
            otherFile.text = "Whatever"
            Log.w("This is for today")

            println "------- Before cleaning [OTHER: ${rotateOthers ? "YES" : "NO"}] -----------"
            fileLogger.logDir.eachFile {
                println " > " + it.name + "\t" + LocalDateTime.fromMillis(it.lastModified()).YMDHmsS
            }
        then:
            // total = create + other (same as create) + link
            assert fileLogger.logDir.listFiles().findAll { it.name.contains("other") }.size() == create + 1
            assert fileLogger.logDir.listFiles().findAll { it.name.contains("test") }.size() == create + 2
        when:
            fileLogger.onCleanDone = {
                println "------- After cleaning -----------"
                fileLogger.logDir.eachFile {
                    println " > " + it.name
                }
                done = true
            }
            fileLogger.cleanLogs()
            while (!done) {
                sleep(50)
            }
        then:
            assert fileLogger.logDir.listFiles().find { it.name == "last-" + fileLogger.logFileName }
            assert fileLogger.logDir.listFiles().findAll { it.name.contains("other") }.size() == (rotateOthers ? [keep, create + 1].min() : create + 1)
            assert fileLogger.logDir.listFiles().findAll { it.name.contains("test") }.size() == [keep, create + 1].min() + 1
            noExceptionThrown()
        where:
            dir | keep | create | expected | rotateOthers
            1   | 2    | 7      | 9        | false
            2   | 7    | 2      | 4        | false
            3   | 2    | 7      | 4        | true
            4   | 7    | 2      | 4        | true
    }

    //---------------- ETC : ZIP --------------
    def "Compressing logs"() {
        when:
            Log.w("Some random warning")
        then:
            assert fileLogger.logFile.exists()
            assert fileLogger.compressLog()
        when:
            def gzLog = new File(fileLogger.logFile.absolutePath + ".gz")
        then:
            assert gzLog.exists()
            assert !fileLogger.logFile.exists()
    }
}