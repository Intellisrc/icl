package com.intellisrc.thread


import spock.lang.Specification

import static com.intellisrc.core.Millis.getSECOND

/**
 * @since 2019/10/11.
 */
class TaskPoolTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.logToFile = false
    }
    def "Reset counters"() {
        setup:
            Tasks.add(IntervalTask.create({
                print "."
            }, "Printer", SECOND, 10))
            sleep(SECOND)
        expect:
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                assert it.executed > 40: "Executed times must be executed several times"
            }
        when:
            Tasks.printStatus()
            println "Resetting........."
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                it.resetCounters()
            }
            Tasks.printStatus()
        then:
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                assert it.executed < 5: "After reset, it should be a low value"
            }
    }
    def "Reset exceptions"() {
        setup:
            int counter = 1
            Tasks.printOnScreen = false
            Tasks.add(IntervalTask.create({
                print "."
                if(counter && counter++ > 50) {
                    throw new Exception("Break it!")
                }
            }, "Printer", SECOND, 10))
            sleep(SECOND)
            counter = 0 //disable exceptions
        expect:
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                assert it.executed > 40: "Executed times must be executed several times"
                if(it.name == "Printer") {
                    assert it.failed > 10: "Failed must be reported several times"
                }
            }
        when:
            Tasks.printStatus()
            println "Resetting........."
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                it.resetCounters()
            }
            Tasks.printStatus()
        then:
            Tasks.taskManager.pools.findAll { it.name.contains("Printer") }.each {
                println "After reset: ${it.executed}"
                assert it.executed < 10: "After reset, it should be a low value"
                if(it.name == "Printer") {
                    assert it.failed == 0: "Failed must have been reset"
                }
            }
        cleanup:
            Tasks.exit()
    }
}