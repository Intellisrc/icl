package com.intellisrc.thread

import com.intellisrc.core.AnsiColor
import groovy.transform.CompileStatic

/**
 * Simple task that will be run once
 * They share pool with tasks with the same name
 *
 * For other cases, please use:
 *
 * IntervalTask : Process to run over and over
 * ParallelTask : Process to be run in several threads (for performance)
 * ServiceTask  : Process to be run forever (blocking)
 *
 * Additionally, this class provides the basic functionality for the above classes
 *
 * @since 2019/08/26.
 *
 */
@CompileStatic
abstract class Task {
    public boolean retry    = false //retry on fail (like queue full)
    public boolean waitResponse = false
    
    protected int minThreads = Tasks.minPoolSize
    protected int maxThreads = Tasks.maxPoolSize
    protected long maxExecutionTime = Tasks.timeout
    protected int sleepTime = 0
    protected Priority priority = Priority.NORMAL

    StateUpdater taskState = (StateUpdater) {} //To be used by TaskInfo
    interface StateUpdater {
        void update(TaskInfo.State State)
    }

    enum Priority {
        MIN(1), LOW(3), NORMAL(5), HIGH(7), MAX(9)
        private int val
        Priority(int value) {
            this.val = value
        }
        int getValue() {
            return val
        }
        String getChar() {
            String color = ""
            switch (this) {
                case MIN :
                    color = AnsiColor.GREEN
                    break
                case LOW :
                    color = AnsiColor.GREEN
                    break
                case NORMAL :
                    color = AnsiColor.YELLOW
                    break
                case HIGH :
                    color = AnsiColor.RED
                    break
                case MAX :
                    color = AnsiColor.RED
                    break
            }
            return color + value + AnsiColor.RESET
        }
    }
    
    /**
     * Inline creation of a simple Task
     * @param runnable
     * @param name
     * @return
     */
    static Task create(final Runnable runnable, final String name, Priority priority = Priority.NORMAL, int maxExecMillis) {
        return new Task() {
            @Override
            Runnable process() throws InterruptedException {
                return runnable
            }
    
            @Override
            String getTaskName() {
                return name
            }
    
            @Override
            Priority getPriority() {
                return priority
            }
    
            @Override
            long getMaxExecutionTime() {
                return maxExecMillis
            }
        }
    }
    
    /**
     * Handle some custom Error or Exception
     * @param ignored
     * @return true if it was handled internally, false if it must be handled by TaskManager
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean onException(Throwable ignored) {
        return false
    }

    //------------------ PRIVATE ----------------
    
    // ----------- IMPORTANT methods --------------
    /**
     * Setup thread (only run once)
     * Be sure that the code inside this method it is
     * not required to reset the state of the thread
     * @return
     */
    void setup() {}
    /**
     * Reset state of thread to start again
     * @return
     */
    boolean reset() { return true }
    /**
     * Process to execute
     * @return Runnable
     * @throws InterruptedException
     * Note: All the code inside that method will be executed in each "step" (not just the return)
     * for code which should run only during initialization, use reset().
     * The main difference is that the code outside the Runnable, will be executed before the Thread
     * has been created (so be careful).
     */
    abstract Runnable process() throws InterruptedException
    /**
     * Close process
     */
    void quit() {}
    //--------------------- OPTIONAL (Override if needed) -------------------
    /**
     * Automatic get class name
     * @return simple name to identify thread
     */
    String getTaskName() {
        return this.class.simpleName
    }
    /**
     * Time between process calls
     * @return
     */
    int getSleepTime() { return this.sleepTime }
    void setSleepTime(int sleep) { this.sleepTime = sleep }
    /**
     * Maximum execution time for the process
     * @return
     */
    long getMaxExecutionTime() { return this.maxExecutionTime }
    void setMaxExecutionTime(long max) { this.maxExecutionTime = max }
    /**
     * Get maximum threads
     * @return
     */
    int getMinThreads() { return this.minThreads }
    void setMinThreads(int min) { this.minThreads = min }
    /**
     * Get maximum threads
     * @return
     */
    int getMaxThreads() { return this.maxThreads }
    void setMaxThreads(int max) { this.maxThreads = max }
    /**
     * Specify priority
     * @return
     */
    Priority getPriority() { return this.priority }
    void setPriority(Priority pri) { this.priority = pri }
}
