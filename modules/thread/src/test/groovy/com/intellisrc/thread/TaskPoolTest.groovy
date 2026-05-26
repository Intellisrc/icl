package com.intellisrc.thread

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.getMILLIS_100
import static com.intellisrc.core.Millis.getSECOND

/**
 * @since 2019/10/11.
 */
class TaskPoolTest extends BaseTaskTest {
    def "Reset counters"() {
        setup:
            int execTime = MILLIS_100
            int maxTime = SECOND
            int countNum = (maxTime / (execTime * 1d)).round().toInteger()
            CountDownLatch counter = new CountDownLatch(countNum + 1)
            Tasks.add(IntervalTask.create({
                print "."
                counter.countDown()
            }, "Printer", maxTime, execTime))
            counter.await(maxTime, TimeUnit.MILLISECONDS)
        expect:
            Tasks.findAll("Printer").each {
                assert it.executed >= countNum: "Executed times must be executed several times"
            }
        when:
            Tasks.printStatus()
            println "Resetting........."
            Tasks.findAll("Printer").each {
                it.resetCounters()
            }
            Tasks.printStatus()
        then:
            Tasks.findAll("Printer").each {
                assert it.executed < countNum: "After reset, it should be a low value"
            }
    }
    def "Reset exceptions"() {
        setup:
            int maxTime = SECOND
            int execTime = MILLIS_100
            int halfTime = (maxTime / (execTime * 2d)).round().toInteger() // Half the time
            AtomicInteger counter = new AtomicInteger()
            Tasks.printOnScreen = false
            Tasks.add(IntervalTask.create({
                print "."
                // Break it half way
                if(counter.getAndIncrement() >= halfTime) {
                    throw new Exception("Break it!")
                }
            }, "Printer", maxTime, execTime))
            sleep(SECOND)
        expect:
            TaskPool printer = Tasks.get("Printer")
            assert printer.executed >= halfTime - 1: "Executed times must be executed several times"
            assert printer.failed >= halfTime - 1: "Failed must be reported several times"
        when:
            Tasks.printStatus()
            println "Resetting........."
            printer.resetCounters()
            Tasks.printStatus()
        then:
            println "After reset: ${printer.executed}"
            assert printer.executed < halfTime: "After reset, it should be a low value"
            assert printer.failed == 0: "Failed must have been reset"
    }
}