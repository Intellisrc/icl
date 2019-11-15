package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * Extend this class for processes which
 * are run in the background forever (like a service)
 *
 * These tasks should BLOCK and the only way out is through interruption
 *
 * Indicated as "*" in Tasks status
 *
 * @since 2019/09/09.
 */
@CompileStatic
abstract class ServiceTask extends Task {
    ServiceTask() {
        maxExecutionTime = 0
        sleepTime = 0
        minThreads = 1
        maxThreads = 1
        retry = true
    }
    
    /**
     * Inline creation of a ServiceTask
     * @param runnable
     * @return
     */
    static ServiceTask create(final Runnable runnable, String name, Priority priority = Priority.NORMAL) {
        return new ServiceTask() {
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
    abstract Runnable process() throws InterruptedException
    
    @Override
    abstract boolean reset()
}
