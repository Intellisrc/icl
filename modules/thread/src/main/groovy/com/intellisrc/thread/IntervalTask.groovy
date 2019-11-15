package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * Extend this class for processes which are intent
 * to be run in the background in intervals (as monitoring)
 *
 * sleepTime is the interval in which this task will be run
 *
 * Indicated as "@" in Tasks status
 *
 * @since 2019/09/09.
 */
@CompileStatic
abstract class IntervalTask extends Task {
    boolean warnOnSkip = true //Turn to off to disable warning about unable to execute Task
    
    IntervalTask(int maxExecutionMillis, int sleepMillis) {
        maxExecutionTime = maxExecutionMillis
        sleepTime = sleepMillis
        minThreads = 1
        maxThreads = 1
        retry = false
    }
    
    /**
     * Inline creation of IntervalTask
     * @param runnable
     * @param maxExecutionMillis
     * @param sleepMillis
     * @return
     */
    static IntervalTask create(final Runnable runnable, String name, int maxExecutionMillis = 1000, int sleepMillis = 1000, Priority priority = Priority.NORMAL) {
        return new IntervalTask(maxExecutionMillis, sleepMillis) {
            @Override
            void setup() {}
    
            @Override
            Runnable process() throws InterruptedException {
                return runnable
            }
    
            @Override
            boolean reset() {
                return true
            }
            
            @Override
            String getTaskName() {
                return name
            }
    
            @Override
            Task.Priority getPriority() {
                return priority
            }
        }
    }
    
    @Override
    abstract void setup()
    
    @Override
    abstract Runnable process() throws InterruptedException
    
    @Override
    abstract boolean reset()
}
