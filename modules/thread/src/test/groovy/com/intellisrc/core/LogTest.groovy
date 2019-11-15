package com.intellisrc.core

import com.intellisrc.thread.TaskInfo
import com.intellisrc.thread.Tasks
import spock.lang.Specification


/**
 * @since 2019/10/15.
 */
class LogTest extends Specification {
    def "Cleanlog with Tasks"() {
        setup:
            Log.cleanLogs(new File("/tmp/"))
            Tasks.printStatus()
            sleep(1000)
        expect:
            assert Tasks.taskManager.pools.find { it.name == "Log.clean" }.executed == 1
    }
}