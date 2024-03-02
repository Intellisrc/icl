//file:noinspection GrFinalVariableAccess
package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.thread.ThreadPool.ErrorCallback
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentLinkedQueue

import static com.intellisrc.core.Millis.getMILLIS_10
import static com.intellisrc.core.Millis.getSECOND

/**
 * Pool controller for tasks
 * A TaskPool works to group tasks and be able
 * to use their own private pool
 *
 * Indicated in Tasks as "#"
 *
 * @since 2019/09/04.
 */
@CompileStatic
class TaskPool implements TaskLoggable {
    final String name
    final ThreadPool executor
    final private ConcurrentLinkedQueue<TaskInfo> taskList = new ConcurrentLinkedQueue<>()
    final private int threads
    
    private int sleepVal = 0
    private int failedVal = 0
    private int executedVal = 0
    private int maxExecVal = 0
    
    String indicator = "-"
    String priority = " "
    TaskInfo.State currStatus = TaskInfo.State.NEW
    TaskInfo.State prevStatus = TaskInfo.State.NEW
    
    // In order of appearance
    LocalDateTime setupTime
    LocalDateTime waitTime
    LocalDateTime startTime
    LocalDateTime failTime
    LocalDateTime doneTime

    TaskPool(String name, int minThreads = 0, int maxThreads = 5, long timeout = SECOND, ErrorCallback onError = {}) {
        this.name = name
        assert minThreads <= maxThreads : "Min threads (" + minThreads + ") can't be equal or grater than max threads (" + maxThreads + ")"
        assert timeout >= 0 : "Timeout can not be negative"
        executor = new ThreadPool(minThreads, maxThreads, timeout, onError)
        threads = maxThreads
        setupTime = SysClock.dateTime
    }
    /**
     * Add one task to the pool to be executed
     * @param info
     */
    void add(final TaskInfo info) {
        taskList << info
        sleepVal = info.sleep
        maxExecVal = info.maxExec
        indicator = info.indicator
        priority = info.task.priority.char
        // Clean unused tasks:
        taskList.removeAll {
            boolean remove = false
            if(it.state == TaskInfo.State.DONE) {
                info.executed += it.executed
                remove = true
            } else if(it.state > TaskInfo.State.NEW && ChronoUnit.MILLIS.between(it.setupTime, SysClock.dateTime) > it.task.maxExecutionTime) {
                remove = true
            }
            if(remove) {
                if(Tasks.debug) {
                    Log.v("[%s] Removing idle task", it.fullName)
                }
            }
            return remove
        }
    }
    /**
     * Retry a task
     * Note: It won't stop its execution
     * @param info
     */
    boolean retry(final TaskInfo info) {
        taskList.remove(info)
        taskList.add(info)
        executor.purge()
        return executor.execute(info)
    }
    
    /**
     * Update timings
     * @param info
     */
    void updateState(final TaskInfo info) {
        prevStatus = currStatus
        currStatus = info.state
        if(Tasks.debug) {
            Log.v("[%s] changed to status: %s -> %s", info.fullName, prevStatus, currStatus)
        }
        switch(currStatus) {
            case TaskInfo.State.SETUP:
                info.setupTime = setupTime = SysClock.dateTime
                info.done = false
                break
            case TaskInfo.State.WAITING:
                info.waitTime = waitTime = SysClock.dateTime
                break
            case TaskInfo.State.RUNNING:
                info.startTime = startTime = SysClock.dateTime
                //----------- TIME OUT -------------
                //Do not timeout monitors:
                if(!info.name.endsWith("-monitor") && info.task.maxExecutionTime) {
                    Tasks.add({
                        while(!(info.done || (info.doneTime && info.startTime <= info.doneTime))) {
                            sleep(MILLIS_10)
                            long timed = ChronoUnit.MILLIS.between(info.startTime, SysClock.dateTime)
                            if (timed > info.task.maxExecutionTime) {
                                Log.w("[%s] Timed out (Took: %d ms)", info.name, timed)
                                info.state = TaskInfo.State.TIMEOUT
                                executor.kill(info)
                                break //Run once
                            }
                        }
                    }, info.name + "-timeout", Task.Priority.MIN, 0) //maxExecute must be 0 or it will overload
                }
                break
            case TaskInfo.State.TERMINATED:
                failedVal++
                info.failTime = failTime = SysClock.dateTime
                info.done = true
                break
            case TaskInfo.State.DONE:
                executedVal++
                info.doneTime = doneTime = SysClock.dateTime
                taskList.remove (info)
                info.done = true
                break
        }
    }
    
    /**
     * Reset execution and failed counters
     * TODO: this is not working properly
     */
    void resetCounters() {
        Log.v("Counters in pool %s will be reset", name)
        executedVal = 0
        failedVal = 0
        // Clear failed before
        taskList.removeAll { it.state == TaskInfo.State.TERMINATED }
        failTime = null
    }
    
    List<TaskInfo> getTasks() {
        return Collections.unmodifiableList(taskList.unique { it.fullName }.sort { it.name })
    }
    
    @Override
    String getFullName() {
        return name
    }
    
    @Override
    String getStatus() {
        // Prioritize custom states against default ones
        return (prevStatus > currStatus && prevStatus > TaskInfo.State.TERMINATED ? prevStatus : currStatus).toString()
    }
    
    @Override
    int getSleep() {
        return sleepVal
    }
    
    @Override
    int getMaxExec() {
        return maxExecVal
    }
    
    @Override
    int getFailed() {
        return failedVal
    }
    @Override
    int getExecuted() {
        return executedVal
    }
    
    @Override
    boolean isRunning() {
        return taskList.empty || taskList.any { it.state != TaskInfo.State.TERMINATED }
    }
}
