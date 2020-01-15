package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic

import java.time.temporal.ChronoUnit
import java.util.concurrent.*

/**
 * A ThreadPoolExecutor which interact with TaskInfo objects
 * It will assign tasks states and manage Thread pools
 *
 * @see: {@link java.util.concurrent.ThreadPoolExecutor}
 *
 * @since 9/3/19.
 */
@CompileStatic
class ThreadPool extends ThreadPoolExecutor {
    private ConcurrentLinkedQueue<ExecutorItem> items = new ConcurrentLinkedQueue<>()
    static interface ErrorCallback {
        void call(TaskInfo info)
    }
    private ErrorCallback onError
    
    static class ExecutorItem {
        int hashID
        final TaskInfo info
        final Runnable runnable
        Thread thread   //used to stop it
        Future future
        Thread timeoutThread
        boolean finished = false
        
        ExecutorItem(final TaskInfo info, final Runnable runnable = null) {
            this.info = info
            this.runnable = runnable ?: info.task.process()
        }
        
        void setFuture(final Future futuristic) {
            this.future = futuristic
            hashID = this.future.hashCode()
            if(Tasks.debug) {
                Log.v("[%s] Adding Item with hash: %d", info.fullName, hashID)
            }
        }
        
        void setThread(final Thread t) {
            if(t) {
                this.thread = t
            }
        }
    }
    
    ThreadPool(int iniPoolSize = 0, int maxPoolSize = 100, long timeoutMillis = 1000L, ErrorCallback onError = {}) {
        super(iniPoolSize, maxPoolSize, timeoutMillis, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>())
        this.onError = onError
    }
    
    /**
     * Submit and ExecutorItem into the thread pool
     * @param executorItem
     * @return
     */
    boolean submit(ExecutorItem executorItem) {
        boolean submitted = false
        boolean rejected = false
        while(!(submitted || rejected)) {
            try {
                executorItem.future = submit(executorItem.runnable)
                submitted = true
            } catch (RejectedExecutionException ignored) {
                Task task = executorItem.info.task
                if (task.retry) {
                    if (task.maxExecutionTime > 0 && ChronoUnit.MILLIS.between(executorItem.info.setupTime, SysClock.dateTime) > task.maxExecutionTime) {
                        Log.w("[%s] Got tired of waiting... it left", executorItem.info.fullName)
                        //TODO: count times it gets rejected so we can adjust pool size
                    } else {
                        executorItem.info.state = TaskInfo.State.WAITING
                        sleep(10)
                    }
                } else {
                    rejected = true
                }
            }
        }
        return submitted
    }
    
    /**
     * Execute a code without thread
     * @param taskInfo
     * @return
     */
    static boolean executeBlocking(final TaskInfo taskInfo) {
        assert taskInfo.task instanceof BlockingTask : "Provided Task is not BlockingTask"
        boolean wasExecuted = false
        try {
            taskInfo.state = TaskInfo.State.SETUP
            taskInfo.task.setup()
            taskInfo.state = TaskInfo.State.RUNNING
            taskInfo.task.process().run()
            wasExecuted = true
            taskInfo.executed++
            taskInfo.state = TaskInfo.State.DONE
        } catch(Exception | Error e) {
            Log.e("Error in thread: %s", TaskInfo.name, e)
            taskInfo.state = TaskInfo.State.TERMINATED
        }
        return wasExecuted
    }
    
    /**
     * Execute a task later
     * @param taskInfo
     * @return
     */
    boolean executeLater(final TaskInfo taskInfo) {
        assert taskInfo.task instanceof DelayedTask : "Provided Task is not DelayedTask"
        boolean scheduled = false
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1)
        taskInfo.state = TaskInfo.State.WAITING
        try {
            ses.schedule({
                execute(taskInfo)
            }, taskInfo.sleep, TimeUnit.MILLISECONDS)
            scheduled = true
        } catch(RejectedExecutionException ignored) {
            Log.w("[%s] was rejected", taskInfo.name)
        }
        ses.shutdown()
        return scheduled
    }
    
    /**
     * Execute several processes in parallel
     * @param taskInfo
     * @return
     */
    boolean executeParallel(final TaskInfo taskInfo) {
        assert taskInfo.task instanceof ParallelTask : "Provided Task is not ParallelTask"
        boolean executed = false
        try {
            List<Runnable> runnables = (taskInfo.task as ParallelTask).processes()
            runnables.each {
                ExecutorItem ei = new ExecutorItem(taskInfo, it)
                submit(ei)
                items << ei
            }
            if (taskInfo.task.waitResponse) {
                items.each {
                    it.future.get(taskInfo.task.maxExecutionTime, TimeUnit.MILLISECONDS)
                }
            }
            executed = true
        } catch(InterruptedException ignored) {
            //Task was interrupted
        } catch(Exception | Error e) {
            Log.e("Error in thread: ", e)
            //It should have been reported inside ThreadPool
        }
        return executed
    }
    /**
     * Executes a task
     * @param taskInfo
     */
    boolean execute(final TaskInfo taskInfo) {
        ExecutorItem ei = items.find { taskInfo.hashCode }
        if(!ei) {
            ei = new ExecutorItem(taskInfo)
        }
        boolean executed = false
        try {
            executed = submit(ei)
            if (executed) {
                //cleanup items:
                try {
                    if (items.size() + 1 > taskInfo.task.maxThreads) {
                        items.remove(items.findAll { it.finished }.sort { it.info.startTime }.first())
                    }
                } catch(NoSuchElementException ignored) {
                    // It got deleted in the process
                }
                items << ei
            }
        } catch(Exception | Error e) {
            Log.e("Error in thread: %s", e)
            //It should have been reported inside ThreadPool
        }
        return executed
    }
    
    /**
     * Before a task is executed
     * @param future
     * @param runnable
     */
    @Override
    protected void beforeExecute(final Thread future, final Runnable runnable) {
        ExecutorItem item = null
        int breaker = -10
        while(!item && breaker++ < 0) {
            item = items.find {
                it.hashID == runnable.hashCode()
            }
            if(!item) {
                sleep(10)
            }
        }
        if (item) {
            item.finished = false
            item.thread = future
            item.info.threadID = future.id
            item.info.done = false
            //Starting...
            item.info.state = TaskInfo.State.RUNNING
            if(!item.info.startTime) {
                if(Tasks.debug) {
                    Log.v("Fixing start time")
                }
                item.info.startTime = SysClock.dateTime
            }
            // Set name and priority
            future.name = item.info.fullName
            if(Tasks.debug) {
                Log.v("[%s] Running task with hash: %s", item.info.fullName, runnable.hashCode())
            }
            future.priority = item.info.task.priority.value
        } else {
            if(Tasks.debug) {
                Log.w("Unable to find Task with ID: %d", runnable.hashCode())
            }
        }
    }
    
    /**
     * After a task is executed
     * @param future
     * @param throwable
     */
    @Override
    protected void afterExecute(final Runnable future, Throwable throwable) {
        final ExecutorItem item = items.find { it.hashID == future.hashCode() }
        if(item) {
            TaskInfo taskInfo = item.info
            if (throwable == null && future instanceof Future<?>) {
                try {
                    if(taskInfo.task.maxExecutionTime) {
                        ((Future<?>) future).get(taskInfo.task.maxExecutionTime, TimeUnit.MILLISECONDS)
                    } else {
                        ((Future<?>) future).get()
                    }
                } catch (Exception | Error e) {
                    throwable = e
                }
            }
            // Turn off timer:
            item.timeoutThread?.interrupt()
            item.finished = true
            try {
                if(throwable) {
                    taskInfo.failed++
                    taskInfo.state = TaskInfo.State.TERMINATED
                    if (taskInfo.task.onException(throwable)) {
                        Log.w("[%s] was terminated", taskInfo.name)
                    } else {
                        if (!taskInfo.task.reset()) {
                            Log.w("[%s] Unable to reset task", taskInfo.name)
                        }
                    }
                    switch (throwable) {
                        case TimeoutException:
                            Log.w("[%s] timed out", taskInfo.fullName)
                            if(item.info.task instanceof Killable) {
                                (item.info.task as Killable).kill()
                                //noinspection GrDeprecatedAPIUsage : Only way to do really kill thread
                                item.thread.stop()
                            }
                            break
                        case CancellationException:
                            Log.w("[%s] was cancelled", taskInfo.fullName)
                            break
                        case ThreadDeath:
                            Log.w("[%s] was killed", taskInfo.fullName)
                            break
                        case InterruptedException:
                            Log.w("[%s] was interrupted", taskInfo.fullName)
                            break
                        default:
                            Log.e("[%s] Exception in task", taskInfo.name, throwable)
                            break
                    }
                    onError.call(taskInfo)
                } else {
                    taskInfo.executed++
                    taskInfo.state = TaskInfo.State.DONE
                    if(!item.info.doneTime) {
                        if(Tasks.debug) {
                            Log.v("Fixing done time")
                        }
                        item.info.doneTime = SysClock.dateTime
                    }
                    if(Tasks.debug) {
                        Log.v("[%s] completed... done.", taskInfo.fullName)
                    }
                }
            } finally {
                items.remove(item)
                if(Tasks.debug) {
                    Log.v("[%s] Removed", item.info.fullName)
                }
            }
        } else if(throwable) {
            Log.e("Unknown thread ended with exception", throwable)
        } else {
            if(Tasks.debug) {
                Log.v("Unable to find Task with hash [%s]", future.hashCode())
            }
        }
    }
    
    /**
     * Kill some task
     * @param info
     */
    boolean kill(TaskInfo info) {
        boolean killed = false
        ExecutorItem item = items.find { it.info.fullName == info.fullName }
        if(item) {
            if(item.future) {
                if(Tasks.debug) {
                    Log.v("[%s] Cancelling task", info.fullName)
                }
                killed = item.future.cancel(true)
                sleep(10)
            } else {
                if(Tasks.debug) {
                    Log.v("[%s] Unable to find future to cancel", info.fullName)
                }
            }
            if(item.thread) {
                if(Tasks.debug) {
                    Log.v("[%s] Interrupting thread", info.fullName)
                }
                item.thread.interrupt()
                if(item.info.task instanceof Killable) {
                    (item.info.task as Killable).kill()
                    sleep(10)
                    Log.i("[%s] Killing thread", info.fullName)
                    //noinspection GrDeprecatedAPIUsage : Only way to do really kill thread
                    item.thread.stop() //Force it to finish
                }
                killed = true
            } else {
                if(Tasks.debug) {
                    Log.v("[%s] Unable to find thread to stop", info.fullName)
                }
            }
        } else {
            if(Tasks.debug) {
                Log.w("[%s] Unable to find item", info.fullName)
            }
        }
        return killed
    }
    
    /**
     * Return the list of Tasks in threadPool
     * @return
     */
    List<TaskInfo> getList() {
        return Collections.unmodifiableList(items.collect { it.info })
    }
    
    /**
     * Remove all items
     */
    @Override
    void purge() {
        terminated()
        super.purge()
    }
    /**
     * When the thread pool is going to quit...
     */
    @Override
    protected void terminated() {
        items.each {
            Log.v("[%s] Exiting...", it.info.name)
            it.info.task.quit()
            it.thread?.interrupt()
            it.future?.cancel(true)
        }
        items.clear()
    }
}
