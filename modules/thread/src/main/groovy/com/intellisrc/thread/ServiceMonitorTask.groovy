package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * @since 2026/01/14.
 */
@CompileStatic
class ServiceMonitorTask extends IntervalTask {
    final Task parent
    final Runnable runnable

    ServiceMonitorTask(Task parent, Runnable runnable, long maxExecutionMillis, int sleepMillis) {
        super(maxExecutionMillis, sleepMillis)
        this.parent = parent
        this.runnable = runnable
        warnOnSkip = false
    }

    @Override
    Runnable process() throws InterruptedException {
        return runnable
    }

    @Override
    String getTaskName() {
        return parent.taskName + "-monitor"
    }
}
