# THREAD Module (ICL.thread)

Manage Tasks (Threads) with priority and watches its performance. You can create parallel 
processes easily, processes which are executed in an interval, as a service or after a specified 
amount of time. This module is very useful to help you to identify bottlenecks and to manage your
main threads in a single place.

[JavaDoc](https://intellisrc.gitlab.io/common/#thread)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/thread)

## Classes

* `Task` : Base class for all Tasks. This is the simplest of all
* `BlockingTask` : Executes a Task in blocking state (not background)
* `DelayedTask` : Executes a Task after N milliseconds
* `IntervalTask` : Executes a Task every N milliseconds
* `ParallelTask` : Executes several Tasks in a parallel pool
* `ServiceTask` : Executes a Task in background that is intended to run forever
* `Killable` : Interface to use to prepare task to be killed
* `Tasks` : Generates report of all tasks and serves as entry point to add tasks to be monitored

## Blocks: RUN (BlockingTask) vs ADD (Task)

It is recommended to create named blocks in your code to help to identify bottlenecks
and other issues.

When using `Tasks.add`, the block of code will be executed in the background, while
using `Tasks.run` will run it in the foreground.

```groovy
class CoolMaker {
    void doSomethingCool() {
        // This task will be added and executed in the background:
        Tasks.add({
            // Creating 10,000 text files
            (1..10000).each {
                File.get("created","files","file-${it}.txt") << "Some content here"
            }
        }, "CoolMaker.newFiles")
        // This task will be added and executed in the foreground (blocking):
        Tasks.run({
            File.get("downloads","pages","downloaded.html") << "https://example.com".toURL().text
        }, "CoolMaker.download")
    }
}
```
The name for each block is up to you. That name will be displayed on the general report (see later below).

## IntervalTask

This task will be executed every N milliseconds.
All the `Task` classes can be extended or created inline, for example:

```groovy
class SuperMonitor extends IntervalTask {
    SuperMonitor() {
        int maxWait = Millis.SECOND
        int interval = Millis.SECOND_5
        super(maxWait, interval)
    }
    /* ... */
}
SuperMonitor superMonitor = new SuperMonitor()
```
Is the same as (inline implementation):
```groovy
IntervalTask superMonitor = IntervalTask.create({
    /* do something */    
}, "SuperMonitor", Millis.SECOND, Millis.SECOND_5)
```

## DelayedTask

The most common way to use this task is:
```groovy
Tasks.runLater({
    /* do something after 10s */    
}, "Runme.Later", Millis.SECOND_10)
```
`runLater` will create and execute a `DelayedTask` after N milliseconds.

## ServiceTask

These tasks are executed on the background, and they will be restarted if
they exit for any reason. All servers (FTP, Web, TCP, Proxy, etc) which
are executed as background services should be executed from a `ServiceTask`.

The following is an example on how to implement a web service as `ServiceTask`:
```groovy
class MuninServiceTask extends ServiceTask {
    @Override
    Runnable process() throws InterruptedException {
        return {
            new WebService(
                port : 3333, 
                resources : "/var/cache/munin/www/"
            ).start() // `start()` will block the thread
        }
    }

    @Override
    boolean reset() {
        // You can do something here if the server exits
        // before restarting again (like checking lock files, etc)
        return true // Return true to specify if service should be restarted
    }
}
```

## ParallelTask

`ParallelTask` will execute several threads at the same time. It will either
execute as many as possible or it will wait until the last thread has been
completed. For example:

```groovy
List<Runnable> processes = []
(1..100).each {
    processes << {
        sleep(Random.range(Millis.SECOND, Millis.SECOND_5)) //Sleep between 1s and 5s
    } as Runnable
}
ParallelTask parallelTask = ParallelTask.create(
        processes,              // A list of processes to execute
        "Parallel.Test",        // Your defined name
        threads,                // Number of threads to use at the same time
        maxTime,                // Kill any process which exceeds this time (milliseconds)
        Task.Priority.NORMAL,   // CPU Priority
        waitToEnd               // If true (usually true) it will wait for all threads to finish before continuing
)
// You can cancel the task during execution:
//parallelTask.cancel()
// Execute the task:
Tasks.add(parallelTask)
```

## Configuration

There are several settings that you can use:
```properties
# file: config.properties
# Where does the log should be stored (by default, same as "log")
tasks.log.dir=log
# Name for the log file (report)
tasks.log.file=tasks.log
# Do not print task summary 
tasks.print=false
# Debug tasks (extra info)
tasks.debug=true
# Print children as well
tasks.children=true
# Save Tasks report into a file (recommended)
# That "log" file is replaced each time
tasks.log=true
```
`Tasks` has more settings, but the above are the more commonly
used.

### Report Example
**NOTE** : Colors are not displayed here:
```
--------------------------------------------------------------------------------------------------------------------------------------------------------
Manager                 | Initialized          | Started                 | Updated                         | Tasks   | Upd   | Run   | Fails
--------------------------------------------------------------------------------------------------------------------------------------------------------
> TaskManager           | 0426 12:17:18.103 1h | 0426 12:17:18.103 1h    | 0426 13:30:10.836               | 4       | 1     | 4     | 0    
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
P Task              TH ∞| Initialized          | Started                 | Finished                Times   | Failed    Times | AvgTm | MaxTm | MaxEx | Sleep | Status 
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
3 Log.clean          1  | 0426 12:17:18.196 1h | 0426 12:17:18.229 33ms  | 0426 12:17:18.245 15ms  1       |                 | 15ms  | 15ms  | 1m    | 0     | DONE
1 Log.clean-timeout  1  | 0426 12:17:18.237 1h | 0426 12:17:18.238 0     | 0426 12:17:18.251 13ms  1       |                 | 13ms  | 13ms  | 0     | 0     | DONE
5 Monitor            1 S| 0426 12:17:18.657 1h | 0426 12:17:18.781 124ms |                                 |                 | 0     | 0     | 0     | 0     | RUNNING
5 Monitor-monitor    1 I| 0426 12:17:18.784 1h | 0426 13:30:10.836 1h    | 0426 13:30:10.686 0     27956   |                 | 475ms | 6s    | 500ms | 150ms | RUNNING
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
```
On the top (where `TaskManager` is displayed) are the general statistics. The details are below that.

#### Detail columns:

* `P` : is the priority in which the task is running.
* `Task` : the name of the task was assigned to it.
* `TH` : number of threads running under that name. Set `tasks.children=true` to see them individually.
* `∞` : type of task: 
  * `B` : BlockingTask
  * `D` : DelayedTask
  * `I` : IntervalTask
  * `S` : ServiceTask
  * `P` : ParallelTask
* `Initialized` : Date (Format: MMDD), Time (with milliseconds), Time since started
* `Started` : Last time this task was started
* `Finished` : Last time this task was finished
* `Times` : How many times this task has been executed
* `Failed , Times` : When was the last time this task failed and how many times
* `AvgTm` : Average execution time for this task 
* `MaxTm` : Maximum execution time for this task (based on history)
* `MaxEx` : Maximum execution time defined for this task (above this, the task may be killed)
* `Sleep` : For `IntervalTask` tasks, this represents the interval
* `Status`: Current status of this task
