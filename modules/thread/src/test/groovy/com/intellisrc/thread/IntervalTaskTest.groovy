package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/10.
 */
class IntervalTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }
    class ProcessTest extends IntervalTask {
        int callTimes = 0
        int processTimeMilliSec = 0
        boolean setupCalled = false
        boolean resetCalled = false
    
        ProcessTest(int maxExecutionMillis, int sleepMillis) {
            super(maxExecutionMillis, sleepMillis)
        }
    
        @Override
        void setup() {
            setupCalled = true
        }
    
        @Override
        Runnable process() {
            return {
                println("Processing Test... Called so far: " + (++callTimes) + " time(s)")
                sleep(processTimeMilliSec)
            }
        }
    
        @Override
        boolean reset() {
            Log.v("Resetting")
            callTimes = 0
            resetCalled = true
            return true
        }
    
        @Override
        Priority getPriority() {
            return Priority.LOW
        }
    }
    
    def "Timings must match"() {
        setup:
            int sleepTimeMilliSecs = HALF_SECOND
            int maxExecutionTime = SECOND
            ProcessTest pt = new ProcessTest(maxExecutionTime, sleepTimeMilliSecs)
            pt.processTimeMilliSec = MILLIS_200
            Tasks.add(pt)
            Log.i("Sleeping ...")
            int wait = SECOND_2
            sleep(wait)
            Log.i("Time's up!")
        expect:
            assert Tasks.taskManager.pools.findAll { it.name.contains("ProcessTest") }.size() == 1 + (maxExecutionTime > 0 ? 1 : 0)
            assert Math.abs(pt.callTimes - (wait / sleepTimeMilliSecs).toFloat().floor()) <= 1
            assert pt.setupCalled
            assert ! pt.resetCalled
            assert Tasks.taskManager.failed == 0
        cleanup:
            Tasks.exit()
    }
    
    class FrozenIntervalTest extends IntervalTask implements TaskKillable {
        int callTimes = 0
        int frozenId = 0
        FrozenIntervalTest(int maxExec, int sleepMillis) {
            super(maxExec, sleepMillis)
        }
        
        @Override
        void setup() {}
    
        @Override
        Runnable process() {
            return {
                Log.d("Processing... (%d)", ++callTimes)
                //new File("/dev/random").text
                final fid = ++frozenId
                //while(threadState != State.TERMINATED) {
                for(int i = 0;; i++) {
                    Log.i("[%s] <%d> Looping (%d) ...", Thread.currentThread().name, fid, i)
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
        void kill() {
            Log.i("Task was killed")
        }
    }
    
    /**
     * Test if a task is successfully stopped.
     * One thing to note in the logs is that there should not be
     * Mixed "Looping..." messages from different threads.
     */
    def "Frozen case"() {
        setup:
            //Turn off detection for this test:
            int maxExec = HALF_SECOND
            int sleepMillis = MILLIS_100
            FrozenIntervalTest ft = new FrozenIntervalTest(maxExec, sleepMillis)
            assert Tasks.add(ft)
            sleep(sleepMillis)
            TaskPool pool = Tasks.taskManager.pools.find {
                it.name == "FrozenIntervalTest"
            }
        expect:
            assert pool : "Pool not found"
        when:
            TaskInfo info = pool.tasks.first()
        then:
            assert info : "Task not found"
            assert Tasks.taskManager.pools.findAll { it.name.contains("Frozen") }.size() == 1 + (maxExec > 0 ? 1 : 0) // timeout
            assert ft.callTimes == 1
        when:
            Log.i("sleeping... ")
            int waitTime = SECOND_2
            sleep(waitTime)
        then:
            Log.i("[%s] Status: %s", info.name, info.state)
            assert ft.frozenId == (waitTime / maxExec).toFloat().round()
            assert ft.callTimes == 1    //Got Reset and its running
            //Even if there is an exception is accounted inside Executor
            assert Math.abs(Tasks.taskManager.failed - ft.frozenId) <= 1 //The last task might be still running
            assert Math.abs(Tasks.taskManager.pools.first().executor.completedTaskCount - ft.frozenId) <= 1
        cleanup:
            Tasks.exit()
    }
    
    def "It should report only those cases in which was actually executed"() {
        setup:
            IntervalTask task = IntervalTask.create({
                println ("Doing...")
                sleep(MILLIS_900)
                println ("Done")
            },"IntervalTest", SECOND, MILLIS_10)
            task.warnOnSkip = false
            Tasks.add(task)
            sleep(SECOND)
        expect:
            assert Tasks.taskManager.pools.findAll { it.name.contains("Interval") }.size() == 2 // + 1 Timeout
            assert Tasks.taskManager.pools.find { it.name.contains("Interval") }.executed < 3
        cleanup:
            Tasks.exit()
    }

    def "Should cancel interval"() {
        setup:
            int counter = 0
            IntervalTask task = IntervalTask.create({
                println (++counter)
            },"IntervalTest", MILLIS_200, MILLIS_100)
            Tasks.add(task)
            sleep(HALF_SECOND)
        when:
            task.cancel()
        then:
            assert task.cancelled
            assert counter > 4 && counter < 8
        when:
            sleep(MILLIS_800)
        then:
            assert counter > 4 && counter < 8
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
    }
}