package com.intellisrc.thread

import com.intellisrc.core.Log

import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/11.
 */
class TaskTest extends BaseTaskTest {
    class FrozenSimpleTest extends Task implements TaskKillable {
        int callTimes = 0
        long maxExecutionTime = HALF_SECOND
        int frozenId = 0
        boolean killed = false
        @Override
        Runnable process() {
            return {
                Log.d("Processing... (%d)", ++callTimes)
                final fid = ++frozenId
                while(!killed) {
                    Log.i("[%s - %d] Looping ...", taskName, fid)
                    sleep(MILLIS_100)
                }
            }
        }
        
        @Override
        boolean reset() {
            Log.i("Resetting")
            callTimes = 0
            return true
        }
    
        @Override
        void onKill() {
            Log.i("Killed")
            killed = true
        }
    }

    def "Frozen case"() {
        setup:
            //Turn off detection for this test:
            FrozenSimpleTest ft = new FrozenSimpleTest()
            assert Tasks.add(ft)
            sleep(MILLIS_100)
            TaskPool pool = Tasks.taskManager.pools.find {
                it.name == "FrozenSimpleTest"
            }
        expect:
            assert pool : "Pool not found"
        when:
            TaskInfo info = pool.tasks.first()
        then:
            assert info : "Task not found"
            assert Tasks.taskManager.pools.findAll { it.name.contains("Frozen") }.size() == 2  // + 1 (Timeout)
            assert ft.callTimes == 1
        when:
            Log.i("sleeping... ")
            int waitTime = SECOND
            sleep(waitTime) //TIMEOUT
        then:
            Log.i("[%s] Status: %s", info.name, info.state)
            TaskPool taskPool = Tasks.taskManager.pools.find { it.name == "FrozenSimpleTest" }
            Tasks.TaskSummary taskSummary = Tasks.summary.find { it.key == "FrozenSimpleTest" }
            assert ft.frozenId == 1
            assert ft.killed
            assert Tasks.taskManager.failed == 1
            assert taskPool.failed == 1
            assert taskPool.executed == 0
            assert taskSummary.average > 0
            assert taskSummary.max > 0
    }
    
    /**
     * Testing if adding many tasks will result in some issue
     */
    def "All threads must be executed but not more than once each"() {
        setup:
            Tasks.printOnChange = true
            AtomicInteger i = new AtomicInteger()
            int threads = 100
            int maxTime = 100
            (1..threads).each {
                final int num ->
                    Tasks.add({
                        int processTime = Random.range(10, maxTime)
                        Log.i("[%s] Starting... %d ms", Thread.currentThread().name, processTime)
                        sleep(processTime)
                        Log.i("[%s] Finished: %d", Thread.currentThread().name, i.incrementAndGet())
                    }, "task" + ":" + num, Task.Priority.NORMAL, 0)
                    sleep(10)
            }
            int wait = threads * maxTime
            int counter = 0
            while(i.get() < threads && counter * 10 < wait) {
                sleep(10)
                counter ++
            }
            Tasks.taskManager.running = false
            sleep(100)
            Log.d("Max threads used: %d / %d", Tasks.taskManager.pools.first().executor.largestPoolSize, Tasks.taskManager.pools.first().executor.maximumPoolSize)
        expect:
            assert i.get() == threads
            assert Tasks.taskManager.failed == 0
            assert Tasks.taskManager.pools.first().executor.largestPoolSize > 0
            assert Tasks.taskManager.pools.first().executor.largestPoolSize <= Tasks.taskManager.pools.first().executor.maximumPoolSize
            //assert Tasks.taskManager.pools.first().tasks.findAll { it.state != TaskInfo.State.DONE }.empty FIXME: not always correct
    }
    
    def "Adding several Tasks with same name, should run them in parallel without waiting"() {
        setup:
            int tasks = 10
            Tasks.printOnChange = true
            (1..tasks).each {
                Tasks.add({
                    sleep(100)
                    Log.i("Done here: $it")
                }, "JustSimple")
            }
            sleep(1200)
        expect:
            assert Tasks.taskManager.failed == 0
            assert Tasks.taskManager.pools.first().executor.completedTaskCount == tasks
    }
}