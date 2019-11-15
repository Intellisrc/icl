package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * Executes a process after N milliseconds
 * @since 2019/09/18.
 */
@CompileStatic
abstract class DelayedTask extends Task {
    abstract Runnable process() throws InterruptedException
    
    DelayedTask(int delayedMillis) {
        maxExecutionTime = 0
        sleepTime = delayedMillis
        //Delayed uses 2 threads as one is called to wait and inside that one, the other is submitted
        minThreads = 2
        maxThreads = 100
        retry = true
        priority = Priority.LOW
    }
    
    static DelayedTask create(final Runnable runnable, String name, int delayedMillis) {
        return new DelayedTask(delayedMillis) {
            @Override
            String getTaskName() {
                return name
            }
            
            @Override
            Runnable process() throws InterruptedException {
                return runnable
            }
        }
    }
}
