package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification


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
            int sleepTime = 100
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
                    sleep(500)
                    one = true
                }, "blocker")
            }
            sleep(100)
            Thread.start {
                Tasks.run({
                    sleep(500)
                    two = true
                }, "blocker")
            }
        when:
            sleep(850)
        then:
            assert one && two
    }
}