package com.intellisrc.thread

import groovy.transform.CompileStatic

import java.time.LocalDateTime
import com.intellisrc.core.SysClock
import static com.intellisrc.core.AnsiColor.*

/**
 * Statistics and related info for Task
 * @since 2019/09/04.
 */
@CompileStatic
class TaskInfo implements TaskLoggable {
    int hashCode = 0
    final String name
    long threadID = 0
    final Task task
    int failed = 0
    int executed = 0
    boolean done = false
    // In order of appearance
    LocalDateTime setupTime
    LocalDateTime waitTime
    LocalDateTime startTime
    LocalDateTime failTime
    LocalDateTime doneTime
    protected State threadState = State.NEW
    protected StateChangeCallback onStateChange = {} as StateChangeCallback
    
    interface StateChangeCallback {
        void call(TaskInfo task)
    }
    
    /**
     * Task States
     *
     * NOTE: States marked with ** are only to display has no effect and are supposed to be set by taskState.update()
     */
    enum State {
        NEW,            // Task is undefined (just created)
        SETUP,          // Before starting
        WAITING,        // Waiting for pool queue to become available
        DONE,           // Task done
        RUNNING,        // Task is being executed
        TERMINATED,     // Task finalized in exception / error
        //---------- Only to display in Tasks:
        CLEANING,       // ** Task is running a cleaning procedure
        EXECUTING,      // ** Task is executing something
        FINISHING,      // ** Task is in the last phase
        PREPARING,      // ** Task is preparing
        RESETTING,      // ** Task is resetting
        RESTARTING,     // ** Task is restarting
        SLEEPING,       // ** Task is in Sleep
        TIMEOUT         // ** Task timed-out
        //------------------------------------
    }
    
    TaskInfo(final Task task) {
        setupTime = SysClock.dateTime
        name = task.taskName
        this.task = task
        //Setup trigger to state change:
        task.taskState = {
            State state ->
                this.state = state
        }
    }
    /**
     * Return task name
     * @return
     */
    @Override
    String getFullName() {
        return name + "-" + (threadID ?: Thread.currentThread().id).toString()
    }
    @Override
    String getIndicator() {
        String indicator
        switch (task) {
            case IntervalTask: indicator    = YELLOW + "I"; break
            case ParallelTask: indicator    = PURPLE + "P"; break
            case ServiceTask: indicator     = GREEN + "S"; break
            case BlockingTask: indicator    = RED + "B"; break
            case DelayedTask: indicator     = CYAN + "D"; break
            default: indicator = " "; break
        }
        return indicator + RESET
    }
    
    @Override
    String getStatus() {
        return state.toString()
    }
    
    @Override
    int getSleep() {
        return task.sleepTime
    }
    
    @Override
    int getMaxExec() {
        return task.maxExecutionTime
    }
/**
     * Return true if task is running
     * @return
     */
    @Override
    boolean isRunning() {
        return state != State.TERMINATED
    }
    /**
     * Return true if the task is new
     * @return
     */
    boolean isNew() {
        return state == State.SETUP
    }
    /**
     * Set State
     * @param state
     */
    protected void setState(State state) {
        //Don't update if they are the same
        if(threadState != state) {
            threadState = state
            onStateChange.call(this)
        }
    }
    protected State getState() {
        return threadState
    }
}
