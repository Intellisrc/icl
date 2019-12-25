package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Controls all background tasks in the system using Fibers
 */
@CompileStatic
class TaskManager {
    //---- Non-Static:
    final private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Tasks.maxPoolSize)
    final private ConcurrentLinkedQueue<TaskPool> taskPools = new ConcurrentLinkedQueue<>()
    LocalDateTime initTime = SysClock.dateTime
    LocalDateTime okTime = SysClock.dateTime
    private AtomicInteger failedCount = new AtomicInteger()
    boolean running = true
    long queueTimeout = 1000L
    /**
     * Setup task
     * @param taskable
     */
    boolean add(final Task task) {
        boolean added = false
        boolean recycled = false
        TaskPool taskPool = null
        if (task) {
            taskPool = taskPools.find {
                it.name == task.taskName
            }
            final TaskInfo taskInfo = new TaskInfo(task)
            if (taskPool) {
                taskPool.add(taskInfo)
                recycled = true
                if(Tasks.debug) {
                    Log.v("[%s] Recycled", taskInfo.fullName)
                }
            } else {
                //Note: The following timeout is about waiting in queue for the thread to be free
                try {
                    taskPool = new TaskPool(
                            task.taskName,
                            task.minThreads,
                            task.maxThreads,
                            queueTimeout, {
                        failedCount.incrementAndGet()
                        okTime = SysClock.dateTime
                    })
                } catch(AssertionError e) {
                    Log.e("Unable to create pool", e)
                    return false
                }
                // Add the pool to the list
                taskPools.add(taskPool)
                // Add the task to the pool
                taskPool.add(taskInfo)
                if(Tasks.debug) {
                    Log.v("[%s] Adding to task monitor", taskInfo.fullName)
                }
            }
            // Update statistics when state change
            taskInfo.onStateChange = {
                final TaskInfo taskChanged ->
                    if(Tasks.debug) {
                        Log.v("[%s] New state: %s", taskChanged.fullName, taskChanged.state)
                    }
                    taskPool.updateState(taskChanged)
                    Tasks.logStatus(taskChanged)
            }
            try {
                // Trigger NEW State
                taskInfo.state = TaskInfo.State.SETUP
                task.setup()
            } catch (Exception | Error e) {
                Log.e("[%s] Unable to setup task", task.taskName, e)
            }
            switch (taskInfo.task) {
                case ServiceTask:
                    Log.i("[%s] Will run as a service", taskInfo.name)
                    if (recycled) {
                        Log.w("[%s] Trying to add a ServiceTask when there is one already running", taskInfo.name)
                    } else {
                        added = (taskPool.executor.execute(taskInfo))
                        IntervalTask monitor = IntervalTask.create({
                            if(running) {
                                //If its a service, run it again
                                if (taskInfo.state == TaskInfo.State.DONE) {
                                    Log.w("[%s] Service exited unexpectedly. Use Tasks.exit() to quit, or return false in reset()", taskInfo.name)
                                    if(taskInfo.task.reset()) {
                                        taskInfo.state = TaskInfo.State.TERMINATED
                                        failedCount.incrementAndGet()
                                    }
                                }
                                if (taskInfo.state == TaskInfo.State.TERMINATED) {
                                    added = taskPool.retry(taskInfo)
                                }
                            }
                        }, taskInfo.name + "-monitor", 500, 150)
                        monitor.warnOnSkip = false
                        add(monitor)
                    }
                    break
                case IntervalTask:
                    Log.i("[%s] Will run under schedule", taskInfo.name)
                    added = scheduledExecutorService.scheduleAtFixedRate({
                        if (running) {
                            if (!taskPool.executor.execute(taskInfo)) {
                                if((taskInfo.task as IntervalTask).warnOnSkip) {
                                    Log.w("[%s] Task was not executed. Disable this warning setting: warnOnSkip to false", taskInfo.name)
                                }
                            }
                        }
                    }, 0, task.sleepTime, TimeUnit.MILLISECONDS)
                    break
                case ParallelTask:
                    if(Tasks.debug) {
                        Log.v("[%s] Will run in multiple threads", taskInfo.name)
                    }
                    added = taskPool.executor.executeParallel(taskInfo)
                    break
                case BlockingTask:
                    if(Tasks.debug) {
                        Log.v("[%s] Will block", taskInfo.name)
                    }
                    added = taskPool.executor.executeBlocking(taskInfo)
                    break
                case DelayedTask:
                    if(Tasks.debug) {
                        Log.v("[%s] Will be executed after %d ms", taskInfo.name, taskInfo.sleep)
                    }
                    added = taskPool.executor.executeLater(taskInfo)
                    break
                default:
                    added = taskPool.executor.execute(taskInfo)
                    break
            }
            if (!added) {
                Log.w("[%s] Task was not executed", taskInfo.name)
            }
        } else {
            Log.e("Task was null")
        }
        return added
    }
    
    /**
     * Read-only list
     * @return
     */
    List<TaskPool> getPools() {
        return Collections.unmodifiableList(taskPools.sort { it.name })
    }
    
    /**
     * Number of failed tasks since starting
     * @return
     */
    int getFailed() {
        return failedCount.get()
    }
    /**
     * Shutdown all processes
     */
    void exit() {
        running = false
        Log.i("ThreadManager is exiting...")
        taskPools.each {
            TaskPool pool ->
                pool.executor.purge()
                pool.executor.shutdownNow()
        }
    }
}
