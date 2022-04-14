package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/17.
 */
class BlockingTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }
    def "blocking tasks"() {
        setup:
            int times = 5
            int called = 0
            int sleepTime = MILLIS_10
            BlockingTask blockingTask = BlockingTask.create({
                sleep(sleepTime)
                Log.i("Blocked %s", ++called)
            }, "blocker")
            (1..times).each {
                Tasks.add(blockingTask)
            }
        expect:
            assert called == times
        cleanup:
            Tasks.exit()
    }
    def "task with same name should be executed"() {
        setup:
            boolean one = false
            boolean two = false
            Thread.start {
                Tasks.run({
                    sleep(HALF_SECOND)
                    one = true
                }, "blocker")
            }
            sleep(MILLIS_100)
            Thread.start {
                Tasks.run({
                    sleep(HALF_SECOND)
                    two = true
                }, "blocker")
            }
        when:
            sleep(MILLIS_800)
        then:
            assert one && two
    }
}