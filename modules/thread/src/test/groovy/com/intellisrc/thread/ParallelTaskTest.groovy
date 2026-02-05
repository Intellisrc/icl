package com.intellisrc.thread

import spock.lang.Retry
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/10.
 */
class ParallelTaskTest extends BaseTaskTest {
    class MiceRace extends ParallelTask {
        static final int laps = 20
        static final int smallTimeSingleLap = MILLIS_10
        static final int ratTimeSingleLap = MILLIS_10 * 2
        static final int bigTimeSingleLap = MILLIS_10 * 3
        volatile boolean smallFinished = false
        volatile boolean bigFinished = false
        volatile boolean ratFinished = false
        int processesNumber = 3 // 3 mice

        MiceRace(int threads) {
            super(threads, 0, true)
        }

        int getSmallTotalTime() { laps * smallTimeSingleLap }
        int getRatatouilleTotalTime() { laps * ratTimeSingleLap }
        int getBigTotalTime() { laps * bigTimeSingleLap }
    
        @Override
        List<Runnable> processes() throws InterruptedException {
            return [
                    {
                        //Small but fast
                        (1..laps).each {
                            print "s"
                            System.out.flush()
                            sleep(smallTimeSingleLap)
                        }
                        print "[s]"
                        System.out.flush()
                        smallFinished = true
                    },
                    {
                        //Big and slow
                        (1..laps).each {
                            print "B"
                            System.out.flush()
                            sleep(bigTimeSingleLap)
                        }
                        print "[B]"
                        System.out.flush()
                        bigFinished = true
                    },
                    {
                        //Ratatouille mouse
                        (1..laps).each {
                            print "r"
                            System.out.flush()
                            sleep(ratTimeSingleLap)
                        }
                        print "[r]"
                        System.out.flush()
                        ratFinished = true
                    }
            ]
        }
    }

    @Retry(delay = 1000)
    @Unroll
    def "All need to get to the goal"() {
        setup:
            MiceRace mr = new MiceRace(threads)
            int smallTime = mr.smallTotalTime
            int bigTime = mr.bigTotalTime
            int ratTime = mr.ratatouilleTotalTime
            Tasks.debug = false // Turn it off for this test
            // Sleep time: more than the minimum less than the maximum (as they must run in parallel)
            int sleepTime = (([smallTime, bigTime, ratTime].sum() as int) - ([smallTime, bigTime, ratTime].min() as int))
            println "Race starting (wait time: $sleepTime) ------------------------"
            System.out.flush()
            Tasks.add(mr)
            sleep(sleepTime)
            println "\nRace finished ---------------------------------------------"
        when:
            TaskPool mouseRacePool = Tasks.get(mr.class.simpleName)
            ThreadPool threadPool = mouseRacePool.executor
        then:
            assert mr.smallFinished && mr.bigFinished && mr.ratFinished
            assert Tasks.taskManager.failed == 0
            assert threadPool.largestPoolSize == [threads, mr.processesNumber].min()
            assert threadPool.completedTaskCount == mr.processes().size()
            assert mouseRacePool.executed >= 1 //TODO: Why in Gitlab Job fails with > 1?
        where:
            threads | unused
            1       | true
            2       | true
            3       | true
            4       | true      // number of largestPoolSize should be 3
    }
    /**
     * Test if tasks will last as long as the timeout
     * increasing the sleep time was causing stack overflow exception.
     * That was fixed in 2.7.4
     */
    @Retry(delay = 1000)
    def "ParallelTask should not expire before time"() {
        setup :
            AtomicInteger times = new AtomicInteger()
            List<Runnable> runnables = []
            (1..20).each {
                int instance ->
                runnables << {
                    println "[$instance] starting ..."
                    (1..10).each {
                        sleep(Random.range(90,130))
                    }
                    
                    println "[$instance] finished in "+times.incrementAndGet()+" place"
                }
            }
            assert runnables.size() == 20
            ParallelTask parallelTask = ParallelTask.create( runnables, "Sleeping", 5, HOUR, Task.Priority.NORMAL, true)
            Tasks.add(parallelTask)
        expect :
            assert times.get() == 20
            
    }

    /**
     * This task will run 2 times (in total 4 tasks), as each task will take 3 seconds.
     * The first 2 tasks will be executed and finish after 3 seconds. Then 2 more tasks
     * will start. On second 5, the "cancel" order will be issued, so no more tasks
     * will be started.
     *
     * @return
     */
    @Retry(delay = 1000)
    def "If task is cancelled, it should not execute pending threads"() {
        setup :
            AtomicInteger times = new AtomicInteger()
            List<Runnable> runnables = []
            (1..6).each {
                int instance ->
                    runnables << {
                        println "[$instance] starting ..."
                        sleep(SECOND_3)
                        println "[$instance] finished in "+times.incrementAndGet()+" place"
                    }
            }
            assert runnables.size() == 6
            ParallelTask parallelTask
            Tasks.runLater({
                parallelTask.cancel()
            }, "Later", SECOND_5)
            parallelTask = ParallelTask.create(runnables, "Sleeping", 2, MIN_20, Task.Priority.NORMAL, true)
            Tasks.add(parallelTask)
        when:
            sleep(SECOND_2)
        then:
            assert parallelTask.cancelled
            assert times.get() == 4
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
    }
}
