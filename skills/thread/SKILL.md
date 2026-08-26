---
name: thread
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for concurrency - background tasks (Task, BlockingTask, DelayedTask, IntervalTask, ParallelTask, ServiceTask), priorities, timeouts that kill runaway tasks, pause/resume/cancel, task pools and monitoring through the Tasks registry and reports. Triggers on com.intellisrc.thread imports or Tasks./IntervalTask/ServiceTask usage in an ICL project. Includes core.
---

# ICL thread Module

Managed concurrency for ICL: create tasks with priorities, timeouts, pausing and statistics, all registered in one static registry (`Tasks`) that can report and control everything. Includes core.

```groovy
implementation 'com.intellisrc:thread:2.10.5'   // + core + groovy
```

Package: `com.intellisrc.thread`. Priority enum: `Task.Priority` `MIN(1), LOW(3), NORMAL(5), HIGH(7), MAX(9)`.

## Task types

| Class | Use | Create |
|---|---|---|
| `Task` | one-shot background | `Task.create(runnable, name, priority, maxExecMillis)` |
| `BlockingTask` | run in foreground | `Tasks.run(runnable, name)` |
| `DelayedTask` | run after N ms | `Tasks.runLater(runnable, name, delayMillis)` |
| `IntervalTask` | run every N ms | `IntervalTask.create(runnable, name, maxExecMillis, sleepMillis, priority)` |
| `ParallelTask` | pool many runnables | `ParallelTask.create(runnables, name, threads, maxExecMillis, priority, waitToEnd)` |
| `ServiceTask` | runs forever, auto-restarts | subclass; override `process()` and `reset()` |

All are controlled with `start()`, `stop()`, `restart()`, `pause()`, `resume()`, `cancel()`, `kill()`, `destroy()`.

## Registry — Tasks

```groovy
Tasks.add(task)                       // register + start (accepts a Collection too)
Tasks.run({ println "foreground" }, "Boot.Init")
Tasks.runLater({ cleanup() }, "App.Clean", 5_000)

Tasks.get("Health.Check")             // TaskPool by name
Tasks.remove("Health.Check")          // cancel + remove
Tasks.report()                        // periodic status log every second (arg = seconds)
Tasks.printStatus()                   // one-shot dump
Tasks.getStatus() ; Tasks.getSummary()// map of counters / per-task averages
```

Tasks with the same name share a pool. Everything added is tracked automatically (`TaskInfo`: state, executed/failed counts, timings).

## Common patterns

```groovy
// Interval: health check every 2s, warn if it takes > 1s
Tasks.add(IntervalTask.create({ checkHealth() }, "Health.Check", 1000, 2000))

// Parallel batch: 10 items, 5 threads, wait until all done
ParallelTask.create((1..10).collect { i -> { process(i) } as Runnable },
                    "Batch.Process", 5, 10_000, Task.Priority.NORMAL, true)

// Service: long-running worker that survives crashes
class Worker extends ServiceTask implements TaskKillable {
    final String taskName = "App.Worker"
    @Override Runnable process() { { ->
        while (true) { job() ; sleep(Millis.SECOND) }
    } }
    @Override boolean reset() { Log.w("worker died, restarting") ; true }  // false = give up
    @Override void onKill()   { cleanup() }       // called when maxExecutionTime kills it
}
Tasks.add(new Worker())
```

## Lifecycle traits

`Task` already implements `TaskCancellable` and `TaskPausable`. Add as needed:

- `TaskKillable` — implement `onKill()`; check `isKilled()` inside long loops.
- `TaskPausable` — override `onPause()`/`onResume()`.
- `TaskCancellable` — override `onCancel()`.
- `TaskLoggable` — custom name/indicator/status for reports.

For timeouts to kill gracefully, implement `TaskKillable` and poll `isKilled()` in loops instead of catching interrupts.

## Pools

- `TaskPool(name, minThreads, maxThreads, timeout, onError)` — grouping/statistics unit; usually implicit via `Tasks`.
- `ThreadPool(iniPoolSize, maxPoolSize, timeoutMillis, onError)` — the executor behind everything; use directly only for custom scheduling (`submit`, `executeLater`, `executeParallel`, `executeBlocking`, `kill`, `purge`).

Also here: `ConfigAutoTask` (schedules `etc.ConfigAuto.update()` — used with AutoConfig) and `ServiceMonitorTask` (auto-created for ServiceTasks).

## Gotchas

- `maxExecutionTime > 0` creates a `name-timeout` monitor that KILLS the task — not a graceful stop; use 0 for unlimited (ServiceTask default).
- Background tasks are daemon threads: the JVM exits when only they remain. Keep a `BlockingTask`/`SysService` alive if the app must stay up.
- `DelayedTask` defaults to LOW priority.
- `Tasks` is a static registry living for the whole JVM; in tests call `Tasks.resetManager()` between specs.
- Interval tasks with `warnOnSkip = true` (default) log a warning when an iteration overruns `maxExecMillis`.
