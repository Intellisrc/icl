package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic

import java.time.LocalDateTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Controls all background tasks in the system using Fibers
 */
@CompileStatic
class TaskManager {
    //---- Non-Static:
    final private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Tasks.maxPoolSize)
    final private ConcurrentLinkedQueue<TaskPool> taskPools = new ConcurrentLinkedQueue<>()
    private AtomicInteger failedCount = new AtomicInteger()
    
    public LocalDateTime initTime = SysClock.dateTime
    public LocalDateTime okTime = SysClock.dateTime
    public boolean running = true
    public long queueTimeout = Millis.SECOND
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
                            queueTimeout,
                     {
                                failedCount.incrementAndGet()
                                okTime = SysClock.dateTime
                            }
                    )
                } catch(AssertionError e) {
                    Log.e("Unable to create pool", e)
                    return false
                }
                // Add the task to the pool
                taskPool.add(taskInfo)
                // Add the pool to the list
                taskPools.add(taskPool)
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
                        if(added) {
                            ServiceTask serviceTask = taskInfo.task as ServiceTask
                            serviceTask.monitor = new ServiceMonitorTask(serviceTask, {
                                if(running) {
                                    if(serviceTask.paused && taskInfo.state != TaskInfo.State.PAUSED) {
                                        Log.i("[%s] Task was paused", serviceTask.taskName)
                                        taskInfo.state = TaskInfo.State.PAUSED
                                    } else if(taskInfo.state == TaskInfo.State.PAUSED &&! serviceTask.paused) { //resume
                                        Log.i("[%s] Task was resumed",serviceTask.taskName)
                                        taskInfo.state = TaskInfo.State.RUNNING
                                    } else if(serviceTask.cancelled) {
                                        Log.i("[%s] Service was cancelled", serviceTask.taskName)
                                        taskInfo.state = TaskInfo.State.CANCELLED
                                        serviceTask.onCancel()
                                        serviceTask.monitor.destroy()
                                    } else {
                                        //If its a service, run it again
                                        if (taskInfo.state == TaskInfo.State.DONE) {
                                            Log.w("[%s] Service exited unexpectedly. Use task.cancel(), Tasks.exit() to quit, or return false in reset()", taskInfo.name)
                                            if (serviceTask.reset()) {
                                                taskInfo.state = TaskInfo.State.TERMINATED
                                                failedCount.incrementAndGet()
                                            }
                                        }
                                        if (taskInfo.state == TaskInfo.State.TERMINATED) {
                                            added = taskPool.retry(taskInfo)
                                        }
                                    }
                                }
                            }, 500, 150)
                            add(serviceTask.monitor)
                        }
                    }
                    break
                case IntervalTask:
                    ScheduledFuture future  //Need to be declared before assign it so its available inside the runnable
                    Log.i("[%s] Will run under schedule", taskInfo.name)
                    IntervalTask intervalTask = (taskInfo.task as IntervalTask)
                    future = scheduledExecutorService.scheduleAtFixedRate({
                        if(intervalTask.paused) {
                            Log.i("[%s] Task was paused", intervalTask.taskName)
                            taskInfo.state = TaskInfo.State.PAUSED
                        } else if(taskInfo.state == TaskInfo.State.PAUSED &&! intervalTask.paused) { //resume
                            Log.i("[%s] Task was resumed", intervalTask.taskName)
                            taskInfo.state = TaskInfo.State.RUNNING
                        } else if(intervalTask.cancelled) {
                            Log.v("Task %s was cancelled", intervalTask.taskName)
                            future.cancel(true)
                            taskInfo.state = TaskInfo.State.CANCELLED
                            intervalTask.onCancel()
                        } else if (running) {
                            if (!taskPool.executor.execute(taskInfo)) {
                                if(intervalTask.warnOnSkip) {
                                    Log.w("[%s] Task was not executed. Disable this warning setting: warnOnSkip to false", taskInfo.name)
                                }
                                intervalTask.onFailure()
                            }
                        }
                    }, 0, task.sleepTime, TimeUnit.MILLISECONDS)
                    added = true
                    break
                case ParallelTask:
                    ParallelTask parallelTask = (taskInfo.task as ParallelTask)
                    if(Tasks.debug) {
                        Log.v("[%s] Will run in multiple threads", taskInfo.name)
                    }
                    if(parallelTask.cancelled) {
                        parallelTask.onCancel()
                    } else {
                        added = taskPool.executor.executeParallel(taskInfo)
                    }
                    break
                case BlockingTask:
                    BlockingTask blockingTask = (taskInfo.task as BlockingTask)
                    if(Tasks.debug) {
                        Log.v("[%s] Will block", taskInfo.name)
                    }
                    if(blockingTask.cancelled) {
                        blockingTask.onCancel()
                    } else {
                        added = taskPool.executor.executeBlocking(taskInfo)
                    }
                    break
                case DelayedTask:
                    DelayedTask delayedTask = (taskInfo.task as DelayedTask)
                    if(Tasks.debug) {
                        Log.v("[%s] Will be executed after %d ms", taskInfo.name, taskInfo.sleep)
                    }
                    if(delayedTask.cancelled) {
                        delayedTask.onCancel()
                    } else {
                        added = taskPool.executor.executeLater(taskInfo)
                    }
                    break
                default:
                    if(taskInfo.task.cancelled) {
                        taskInfo.task.onCancel()
                    } else {
                        added = taskPool.executor.execute(taskInfo)
                    }
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
     * Remove a taskPool (it must not be running)
     * @param taskPool
     * @return
     */
    protected boolean remove(TaskPool taskPool) {
        if(taskPool.running &&! taskPool.cancelled) {
            Log.w("TaskPool [%s] was running but was removed from pools.", taskPool.fullName)
        }
        return taskPools.remove(taskPool)
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
