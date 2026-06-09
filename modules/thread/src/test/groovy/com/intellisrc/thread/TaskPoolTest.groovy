package com.intellisrc.thread

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/10/11.
 */
class TaskPoolTest extends BaseTaskTest {
    def "Reset counters"() {
        setup:
            int execTime = MILLIS_100
            int targetExecutions = 10
            int maxLifeTime = SECOND * 2

            CountDownLatch counter = new CountDownLatch(targetExecutions)
            Tasks.add(IntervalTask.create({
                print "."
                counter.countDown()
            }, "Printer", maxLifeTime, execTime))

            // Wait for the 10th execution to trigger
            counter.await(SECOND * 3, TimeUnit.MILLISECONDS)
            sleep(MILLIS_100) // <--- FIX: Let the runner finish updating 'executed' stats
        expect:
            Tasks.findAll("Printer").each {
                assert it.executed >= targetExecutions: "Executed times must be executed several times"
            }
            // ... rest of the test remains the same ...
    }
    def "Reset exceptions"() {
        setup:
            int maxTime = SECOND * 2
            int execTime = MILLIS_100
            int halfTime = 5
            AtomicInteger counter = new AtomicInteger()
            CountDownLatch exceptionLatch = new CountDownLatch(1)

            Tasks.printOnScreen = false
            Tasks.add(IntervalTask.create({
                print "."
                if(counter.getAndIncrement() >= halfTime) {
                    exceptionLatch.countDown()
                    throw new Exception("Break it!")
                }
            }, "Printer", maxTime, execTime))

            // Wait for the exception to be thrown
            exceptionLatch.await(SECOND * 3, TimeUnit.MILLISECONDS)
            sleep(MILLIS_100) // <--- FIX: Let the runner catch the exception and update 'failed' stats
        expect:
            TaskPool printer = Tasks.get("Printer")
            assert printer.executed >= halfTime: "Executed times must be executed several times"
            assert printer.failed >= 1: "Failed must be reported"
            // ... rest of the test remains the same ...
    }
}