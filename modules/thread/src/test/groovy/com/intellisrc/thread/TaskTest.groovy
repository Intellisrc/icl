package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * @since 2019/09/11.
 */
class TaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }
    class FrozenSimpleTest extends Task implements TaskKillable {
        int callTimes = 0
        long maxExecutionTime = 500
        int frozenId = 0
        @Override
        Runnable process() {
            return {
                Log.d("Processing... (%d)", ++callTimes)
                //new File("/dev/random").text
                final fid = ++frozenId
                //while(threadState != State.TERMINATED) {
                for(;;) {
                    Log.i("[%s - %d] Looping ...", taskName, fid)
                    sleep(100)
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
        void kill() {
            Log.i("Killed")
        }
    }
    
    def "Frozen case"() {
        setup:
            //Turn off detection for this test:
            FrozenSimpleTest ft = new FrozenSimpleTest()
            assert Tasks.add(ft)
            sleep(100)
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
            int waitTime = 1000
            sleep(waitTime)
        then:
            Log.i("[%s] Status: %s", info.name, info.state)
            assert ft.frozenId == 1
            assert ft.callTimes == 0    //Got Reset
            assert Tasks.taskManager.failed == 1
            assert Tasks.taskManager.pools.find { it.name == "FrozenSimpleTest" }.failed == 1
            //Even if there is an exception is accounted inside Executor
            assert Tasks.taskManager.pools.find { it.name == "FrozenSimpleTest" }.executor.completedTaskCount == 1
            assert Tasks.summary.find { it.key == "FrozenSimpleTest" }.average > 0
            assert Tasks.summary.find { it.key == "FrozenSimpleTest" }.max > 0
        cleanup:
            Tasks.exit()
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
            assert Tasks.taskManager.pools.first().tasks.findAll { it.state != TaskInfo.State.DONE }.empty
        cleanup:
            Tasks.exit()
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
        cleanup:
            Tasks.exit()
            
    }
}