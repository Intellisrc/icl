package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * This is a Task that will be executing in the main thread
 * The reason of this class is to allow Tasks to monitor and track the code inside
 *
 * NOTE: This Task can not be cancelled, just interrupted (which may leave it in an unknown state)
 * In order to cancel it, you need to implement you own logic.
 *
 * @since 2019/09/17.
 */
@CompileStatic
abstract class BlockingTask extends Task {
    abstract Runnable process() throws InterruptedException
    
    BlockingTask() {
        maxExecutionTime = 0
        sleepTime = 0
        minThreads = 1
        maxThreads = 1
    }
    
    static BlockingTask create(final Runnable runnable, String name) {
        return new BlockingTask() {
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
