package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import spock.lang.Retry
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.intellisrc.core.Millis.*
import static com.intellisrc.core.Millis.HALF_SECOND
import static com.intellisrc.core.Millis.HALF_SECOND
import static com.intellisrc.core.Millis.MILLIS_200
import static com.intellisrc.core.Millis.MILLIS_800


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
            }, "TestLater", SECOND)
        expect:
            assert !called
            sleep(MILLIS_100)
            assert !called
            sleep(SECOND)
            assert called
        cleanup:
            Tasks.exit()
    }
    def "Multiple delayed processes"() {
        setup:
            int called = 0
            int times = 4
            (1..times).each {
                sleep(MILLIS_300)
                Tasks.runLater({
                    called++
                }, "TestLater", SECOND_2)
            }
            Log.i("Setup ready")
        expect:
            // Note: sleep() is not exact
            // Tasks should be called after 2000, 2300, 2600 and 2900 secs
            assert !called : "At the beginning it should not be called yet"
            sleep(HALF_SECOND + MILLIS_200)
            Log.i("Just a moment..")
            assert !called : "After few ms, still should not be called"
            //TODO: Due to inaccuracy of sleep, we can't test reliable some middle point
            sleep(SECOND_3)
            Log.i("All must be done")
            assert called == times : "At the end all should have been called"
        cleanup:
            Tasks.exit()
    }
    class DelayedTest extends DelayedTask {
        boolean called = false
        boolean onPauseCalled = false
        String taskName = "DelayTest"
        CountDownLatch pausedLatch = new CountDownLatch(1)
        CountDownLatch executedLatch = new CountDownLatch(1)

        DelayedTest(int delayedMillis) {
            super(delayedMillis)
        }

        @Override
        void onPause() {
            onPauseCalled = true
            pausedLatch.countDown()
        }

        @Override
        Runnable process() throws InterruptedException {
            return {
                called = true
                executedLatch.countDown()
                Log.i("Method was executed")
            }
        }
    }
    def "Cancel a delayed process"() {
        setup:
            DelayedTest delayedTest = new DelayedTest(SECOND)
            Tasks.add(delayedTest)
        expect:
            assert !delayedTest.called : "Starting, it should not be called"
        when:
            sleep(HALF_SECOND)
            delayedTest.cancel()
        then:
            sleep(SECOND)
            assert !delayedTest.called : "Ending: it should have been cancelled"
            assert delayedTest.cancelled
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
            Tasks.exit()
    }
    def "Pause a delayed process before execution"() {
        setup:
            DelayedTest delayedTest = new DelayedTest(SECOND)
            Tasks.add(delayedTest)
        expect:
            assert !delayedTest.called : "Starting, it should not be called"
        when:
            delayedTest.pause()
        then:
            assert delayedTest.paused
        when:
            delayedTest.pausedLatch.await(HALF_SECOND, TimeUnit.MILLISECONDS)
        then:
            assert delayedTest.onPauseCalled
            assert !delayedTest.called : "Should not be called"
            assert !delayedTest.cancelled
        when:
            delayedTest.resume()
            delayedTest.executedLatch.await(SECOND, TimeUnit.MILLISECONDS)
        then:
            assert !delayedTest.paused
            assert delayedTest.called : "Should be called now"
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
            Tasks.exit()
    }
}