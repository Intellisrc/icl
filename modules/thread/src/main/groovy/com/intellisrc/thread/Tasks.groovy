package com.intellisrc.thread

import static com.intellisrc.core.AnsiColor.*
import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.etc.TimeClock
import com.intellisrc.thread.Task.Priority
import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * This class is used to simplify access to TaskManager
 * @since 2019/08/26.
 */
@CompileStatic
class Tasks {
    static String logFileName = Config.get("tasks.logfile", "status.log")
    static int minPoolSize = Config.getInt("tasks.pool.min", 1)
    static int maxPoolSize = Config.getInt("tasks.pool.max", 30)
    static int bufferMillis = Config.getInt("tasks.buffer", 1000)
    static int timeout = Config.getInt("tasks.timeout", 1000)
    static boolean logToFile = Config.getBool("tasks.log")
    static boolean debug = Config.getBool("tasks.debug")
    static boolean resetTasks = !Config.getBool("tasks.noreset")
    static boolean printChilds = Config.getBool("tasks.children")
    static boolean printOnChange = false    //Using in UnitTests to debug
    protected static TaskManager taskManager = new TaskManager()
    private static LocalDateTime logLastUpdated = LocalDateTime.now()
    private static ConcurrentLinkedQueue<String> logUpdatedList = new ConcurrentLinkedQueue<>()
    private static ConcurrentLinkedQueue<TaskSummary> summary = new ConcurrentLinkedQueue<>()
    
    static {
        // Launch reset task
        if (resetTasks) {
            resetTask()
        }
    }
    
    static class TaskSummary {
        final String key
        private double avgTime = 0
        private long maxTime = 0
        private long times = 0
        
        TaskSummary(String key) {
            this.key = key
        }
        
        void add(long millisElapsed) {
            if (millisElapsed) {
                double currSum = avgTime * times
                double newSum = currSum + millisElapsed
                avgTime = newSum / ++times
                if (millisElapsed > maxTime) {
                    maxTime = millisElapsed
                }
            }
        }
        
        long getAverage() {
            return Math.round(avgTime).toLong()
        }
        
        long getMax() {
            return maxTime
        }
    }
    /**
     * Reset taskManager
     * (Used mainly in Unit Testing)
     */
    static void resetManager() {
        taskManager = new TaskManager()
    }
    
    private static File getLogFile() {
        File baseDir = Log.directory ?: new File(SysInfo.userDir, "log")
        return new File(baseDir, LocalDate.now().YMD + "-" + logFileName)
    }
    
    /**
     * Run a process on the background (SimpleTask).
     * @param simpleProcess : Runnable
     * @param name
     */
    static boolean add(Runnable simpleProcess, String name, Priority priority = Priority.NORMAL, int maxExecuteMillis = 1000) {
        taskManager.add(Task.create(simpleProcess, name, priority, maxExecuteMillis))
    }
    static boolean add(Task task) {
        return add([task])
    }
    
    static boolean add(List<Task> tasks) {
        boolean added = false
        tasks.each {
            added = taskManager.add(it)
        }
        return added
    }
    /**
     * Run a blocking task immediately
     * These blocking tasks, are not background processes and have no timeout or priority
     *
     * @param blockingProcess : Runnable
     * @param name
     */
    static boolean run(Runnable blockingProcess, String name) {
        taskManager.add(BlockingTask.create(blockingProcess, name))
    }
    /**
     * Run some process after N milliseconds
     * @param process
     * @param name
     * @param afterMillis
     * @return
     */
    static boolean runLater(Runnable delayedProcess, String name, int afterMillis) {
        taskManager.add(DelayedTask.create(delayedProcess, name, afterMillis))
    }
    
    static private String getLogDate(LocalDateTime time) {
        return time.format("MMdd HH:mm:ss.SSS").padRight(17)
    }
    /**
     * Return monitoring process
     * Prints log delayed or on change
     * @return
     */
    static void logStatus(final TaskInfo changedTask = null) {
        LocalDateTime now = LocalDateTime.now()
        if (changedTask) {
            TaskSummary summ = summary.find { it.key == changedTask.name }
            if (!summ) {
                summ = new TaskSummary(changedTask.name)
                summary << summ
            }
            switch (changedTask.state) {
                case TaskInfo.State.DONE:
                    if (changedTask.startTime && changedTask.doneTime) {
                        summ.add(ChronoUnit.MILLIS.between(changedTask.startTime, changedTask.doneTime))
                    } else {
                        if (!changedTask.startTime) {
                            Log.w("[%s] had no start time", changedTask.fullName)
                        }
                        if (!changedTask.doneTime) {
                            Log.w("[%s] had no done time", changedTask.fullName)
                        }
                    }
                    break
                case TaskInfo.State.TERMINATED:
                    if (changedTask.startTime && changedTask.failTime) {
                        summ.add(ChronoUnit.MILLIS.between(changedTask.startTime, changedTask.failTime))
                    } else {
                        if (!changedTask.startTime) {
                            Log.d("[%s] had no start time", changedTask.fullName)
                        }
                        if (!changedTask.failTime) {
                            Log.d("[%s] had no fail time", changedTask.fullName)
                        }
                    }
                    break
            }
        }
        if (printOnChange) {
            if (changedTask) {
                if (!logUpdatedList.contains(changedTask.name)) {
                    logUpdatedList << changedTask.name
                }
            }
        } else {
            if (ChronoUnit.MILLIS.between(logLastUpdated, now) > bufferMillis) {
                logLastUpdated = now
            } else {
                if (changedTask) {
                    if (!logUpdatedList.contains(changedTask.name)) {
                        logUpdatedList << changedTask.name
                    }
                }
                return
            }
        }
        printStatus()
    }
    
    /**
     * Print status log immediately
     */
    static void printStatus() {
        final int SEP = 3
        String log = ""
        
        List<TaskPool> list = taskManager.pools
        int headerSize = (2 + 38 + SEP + 23 + SEP + 23 + SEP + 23 + SEP + 7 + SEP + 5 + SEP + 5 + SEP + 5)
        // Header
        log += ("-" * headerSize) + "\n"
        log += "Manager".padRight(39) + " | " + "Initialized".padRight(23) + " | " + "Started".padRight(23) + " | " + "Updated".padRight(23) + " | " + "Tasks  " + " | " + "Upd  " + " | " + "Run  " + " | " + "Fails" + "\n"
        log += ("-" * headerSize) + "\n"
        // TaskManager:
        boolean allChanged = list.size() == logUpdatedList.size()
        log += (allChanged ? GREEN : YELLOW) + "> " + TaskManager.class.simpleName.padRight(37) + RESET + " | " +
                getLogDate(taskManager.initTime) + " " +
                CYAN + TimeClock.getTimeSince(taskManager.initTime).padRight(5) + RESET + " | " +
                getLogDate(taskManager.okTime) + " " +
                CYAN + TimeClock.getTimeSince(taskManager.okTime).padRight(5) + RESET + " | " +
                getLogDate(logLastUpdated) + (" " * 6) + " | " +
                YELLOW + list.size().toString().padRight(7) + RESET + " | " +
                GREEN + logUpdatedList.size().toString().padRight(5) + RESET + " | " +
                GREEN + list.findAll { it.running }.size().toString().padRight(5) + RESET + " | " +
                RED + taskManager.failed.toString().padRight(5) + RESET + "\n"
        // Tasks Separator
        int infoSize = (2 + 38 + SEP + 23 + SEP + 23 + SEP + 31 + SEP + 29 + SEP + 5 + SEP + 5 + SEP + 5 + SEP + 5 + SEP + 5 + SEP + 8)
        log += ("-" * infoSize) + "\n"
        log += "P " + "Task".padRight(34) + "TH ∞| " + "Initialized".padRight(23) + " | " +
                "Started".padRight(23) + " | " + "Finished".padRight(23) + " Times  " + " | " +
                "Failed".padRight(23) + " Times" + " | " + "AvgTm" + " | " + "MaxTm" + " | " + "MaxEx" + " | " + "Sleep" + " | " + "Status" + " \n"
        log += ("-" * infoSize) + "\n"
        list.each {
            TaskPool pool ->
                log += getRow(pool)
                if (printChilds) {
                    pool.tasks.each {
                        TaskInfo info ->
                            log += getRow(info)
                    }
                }
        }
        log += ("-" * infoSize) + "\n"
        if (logToFile) {
            logFile.text = log
        } else {
            print(log)
        }
    }
    
    /**
     * Return a row in the log
     * @param item
     * @return
     */
    static String getRow(TaskLoggable item) {
        //boolean changed = changedTask && item.task.taskName == changedTask.taskName
        boolean changed = logUpdatedList.any { it == item.name }
        boolean isPool = item instanceof TaskPool
        TaskPool pool = isPool ? (item as TaskPool) : null
        
        TaskSummary summ = isPool ? summary.find { it.key == item.name } : null
        logUpdatedList.remove(item.name)
        //boolean changed = changedTask && item.task.taskName == changedTask.taskName
        String name = (changed ? GREEN : YELLOW) + item.fullName.padRight(35) + (isPool ? pool.executor.largestPoolSize.toString() : " ").padRight(2)
        return (isPool ? pool.priority : "└") + RESET + " " + name + RESET + (isPool ? item.indicator : " ") + "| " +
                (item.setupTime ? getLogDate(item.setupTime) + " " + CYAN + TimeClock.getTimeSince(item.setupTime).padRight(5) : " " * 23) + RESET + " | " +
                // waitTime not logged for now
                (item.startTime ? getLogDate(item.startTime) + " " + CYAN + TimeClock.getTimeSince(item.setupTime, item.startTime).padRight(5) : " " * 23) + RESET + " | " +
                (item.doneTime ? getLogDate(item.doneTime) + " " + CYAN + TimeClock.getTimeSince(item.startTime, item.doneTime).padRight(5) + RESET + " " + item.executed.toString().padRight(7) : " " * 31) + RESET + " | " +
                (item.failTime ? RED + getLogDate(item.failTime) + " " + CYAN + TimeClock.getTimeSince(item.failTime).padRight(5) + " " + RED + item.failed.toString().padRight(5) : " " * 29) + RESET + " | " +
                (summ ? dangerColor(summ.average, 500, item.maxExec ?: 1000) : " ".padRight(5)) + " | " +
                (summ ? dangerColor(summ.maxTime, 500, item.maxExec ?: 1000) : " ".padRight(5)) + " | " +
                TimeClock.millisToString(item.maxExec).padRight(5) + " | " +
                TimeClock.millisToString(item.sleep).padRight(5) + " | " +
                (item.running ? (changed ? GREEN : YELLOW) : RED) + item.status + RESET + "\n"
    }
    
    static String dangerColor(long value, long warnValue, long dangerValue) {
        return RESET + ((value >= dangerValue ? RED : (value >= warnValue ? YELLOW : "")) + TimeClock.millisToString(value).padRight(5)) + RESET
    }
    
    static void exit() {
        taskManager.exit()
    }
    
    static void block() {
        while (taskManager.running) {
            sleep(100)
        }
    }
    
    static void report(int each = 1000) {
        add(IntervalTask.create({
            logStatus()
        }, "Tasks.report", each, each, Priority.LOW))
    }
    
    /**
     * Clear counters
     */
    private static void resetTask() {
        add(IntervalTask.create({
            if (ChronoUnit.SECONDS.between(LocalDateTime.MIN, LocalDateTime.now()) <= 1) {
                taskManager.pools.each {
                    TaskPool pool ->
                        pool.resetCounters()
                }
            }
        }, "Tasks.reset", 1000, 1000, Priority.MIN))
    }
    
    static List<TaskSummary> getSummary() {
        return Collections.unmodifiableList(summary.toList())
    }
    
    static Map getStatus() {
        List<TaskPool> list = taskManager.pools
        return [
                initTime: taskManager.initTime.YMDHmsS,
                okTime  : taskManager.okTime.YMDHmsS,
                updated : logLastUpdated.YMDHmsS,
                tasks   : list.size(),
                failed  : taskManager.failed
        ]
    }
}
