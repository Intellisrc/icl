package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification


/**
 * @since 2019/09/18.
 */
class DelayedTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }
    def "Delay some process"() {
        setup:
            boolean called = false
            Tasks.runLater({
                called = true
            }, "TestLater", 1000)
        expect:
            assert !called
            sleep(100)
            assert !called
            sleep(1000)
            assert called
        cleanup:
            Tasks.exit()
    }
    def "Multiple delayed processes"() {
        setup:
            int called = 0
            int times = 4
            (1..times).each {
                sleep(300)
                Tasks.runLater({
                    called++
                }, "TestLater", 2000)
            }
            Log.i("Setup ready")
        expect:
            // Note: sleep() is not exact
            // Tasks should be called after 2000, 2300, 2600 and 2900 secs
            assert !called : "At the beginning it should not be called yet"
            sleep(700)
            Log.i("Just a moment..")
            assert !called : "After few ms, still should not be called"
            //TODO: Due to inaccuracy of sleep, we can't test reliable some middle point
            sleep(3000)
            Log.i("All must be done")
            assert called == times : "At the end all should have been called"
        cleanup:
            Tasks.exit()
    }
}