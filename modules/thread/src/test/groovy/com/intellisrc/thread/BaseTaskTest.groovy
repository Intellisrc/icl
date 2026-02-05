package com.intellisrc.thread

import spock.lang.Specification

import static com.intellisrc.core.Millis.getSECOND

/**
 * @since 2026/02/05.
 */
abstract class BaseTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
        Tasks.debug = true
        Tasks.taskManager.pools.each {
            assert it.executor.list.empty
        }
    }
    def cleanup() {
        Tasks.exit()
        sleep(SECOND) //Wait for all tasks to finish before continue
    }
}
