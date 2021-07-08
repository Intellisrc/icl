package com.intellisrc.thread

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

/**
 * Extend this class for process to be run
 * in parallel (to improve performance)
 *
 * Indicated as "=" in Tasks status
 *
 * @since 2019/09/09.
 */
@CompileStatic
abstract class ParallelTask extends Task implements TaskCancellable {
    ParallelTask(int threads, long maxExecutionMillis = 0, boolean waitToEnd = false) {
        minThreads = 1
        maxThreads = threads
        retry = true
        maxExecutionTime = maxExecutionMillis
        waitResponse = waitToEnd
    }
    
    /**
     * Inline creation of ParallelTask (multiple processes)
     * @param runnable
     * @param threads
     * @param maxExecutionMillis
     * @return
     */
    static ParallelTask create(final List<Runnable> runnables, String name, int threads = 5, int maxExecutionMillis = 1000, Priority priority = Priority.NORMAL, boolean waitToEnd = false) {
        return new ParallelTask(threads, maxExecutionMillis, waitToEnd) {
            @Override
            String getTaskName() {
                return name
            }
    
            @Override
            Task.Priority getPriority() {
                return priority
            }
    
            @Override
            List<Runnable> processes() throws InterruptedException {
                return runnables
            }
        }
    }
    /**
     * Inline creation of ParallelTask (single process)
     * @param runnable
     * @param threads
     * @param maxExecutionMillis
     * @return
     */
    static ParallelTask create(final Runnable runnable, String name, int threads = 5, int maxExecutionMillis = 1000, Priority priority = Priority.NORMAL, boolean waitToEnd = false) {
        return new ParallelTask(threads, maxExecutionMillis, waitToEnd) {
            @Override
            String getTaskName() {
                return name
            }
            
            @Override
            Task.Priority getPriority() {
                return priority
            }
            
            @Override
            List<Runnable> processes() throws InterruptedException {
                return [runnable]
            }
        }
    }
    
    abstract List<Runnable> processes() throws InterruptedException

    @Override
    Runnable process() throws InterruptedException {
        return { Log.w("[%s] has no process", taskName) }
    }
}
