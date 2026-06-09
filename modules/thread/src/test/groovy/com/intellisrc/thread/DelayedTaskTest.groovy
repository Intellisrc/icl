package com.intellisrc.thread

import com.intellisrc.core.Log

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/18.
 */
class DelayedTaskTest extends BaseTaskTest {
    def "Delay some process"() {
        setup:
            CountDownLatch latch = new CountDownLatch(1)
            long startTime = System.currentTimeMillis()
            long executionTime = 0

            Tasks.runLater({
                executionTime = System.currentTimeMillis() - startTime
                latch.countDown()
            }, "TestLater", SECOND)
        expect:
            // 1. Verify it hasn't run prematurely
            sleep(MILLIS_100)
            assert latch.count == 1 : "Should not execute prematurely"

            // 2. Wait up to 3 seconds for the execution to complete
            boolean completed = latch.await(SECOND * 3, TimeUnit.MILLISECONDS)
            assert completed : "Task failed to execute within timeout"

            // 3. Assert that the delay was reasonably accurate.
            // On a slow CI, we allow a small scheduling overhead buffer (e.g., 300ms)
            assert executionTime >= SECOND : "Executed too early: ${executionTime}ms"
            assert executionTime < SECOND + MILLIS_300 : "Executed too late: ${executionTime}ms"
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
    }
    class DelayedTest extends DelayedTask {
        boolean called = false
        boolean onPauseCalled = false
        boolean onResumeCalled = false
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
        void onResume() {
            Log.i("Resuming..")
            onResumeCalled = true
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
    }
    def "Pause a delayed process before execution"() {
        setup:
            DelayedTest delayedTest = new DelayedTest(SECOND)
            Tasks.add(delayedTest)
        expect:
            assert !delayedTest.called : "Starting, it should not be called"
        when:
            sleep(MILLIS_100)
            delayedTest.pause()
            delayedTest.pausedLatch.await(HALF_SECOND, TimeUnit.MILLISECONDS)
        then:
            assert delayedTest.onPauseCalled
            assert delayedTest.paused
            assert !delayedTest.called : "Should not be called"
            assert !delayedTest.cancelled
        when:
            sleep(waitTime)
            delayedTest.resume()
            delayedTest.executedLatch.await(SECOND_3, TimeUnit.MILLISECONDS)
        then:
            assert delayedTest.onResumeCalled
            assert !delayedTest.paused
            assert delayedTest.called : "Should be called now"
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
        where:
            // When it is shorter than delay time, it should resume without taking more time
            // otherwise, it should wait until resume() is called
            waitTime    | unused
            MILLIS_100  | false
            SECOND_2    | false
    }
}