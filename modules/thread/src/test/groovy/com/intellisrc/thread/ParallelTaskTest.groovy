package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/10.
 */
class ParallelTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }

    class MouseRace extends ParallelTask {
        boolean smallFinished = false
        boolean bigFinished = false
        boolean ratFinished = false
        
        static int pool = 2
        
        MouseRace() {
            super(pool, 0, true)
        }
    
        @Override
        List<Runnable> processes() throws InterruptedException {
            return [
                    {
                        //Small but fast
                        (1..20).each {
                            print "s"
                            sleep(MILLIS_10)
                        }
                        print "[s]"
                        smallFinished = true
                    },
                    {
                        //Big and slow
                        (1..20).each {
                            print "B"
                            sleep(MILLIS_10 * 3)
                        }
                        print "[B]"
                        bigFinished = true
                    },
                    {
                        //Ratatouille mouse
                        (1..20).each {
                            print "r"
                            sleep(MILLIS_10 * 2)
                        }
                        print "[r]"
                        ratFinished = true
                    }
            ]
        }
    }
    
    def "All need to get to the goal"() {
        setup:
            int smallTime = 20*10
            int bigTime = 20*30
            int ratTime = 20*20
            MouseRace mr = new MouseRace()
            Tasks.add(mr)
            // Sleep time: more than the minimum less than the maximum
            int sleepTime = (([smallTime, bigTime, ratTime].sum() as int) - ([smallTime, bigTime, ratTime].min() as int))
            Log.i("Waiting... %d ms", sleepTime)
            sleep(sleepTime)
            ThreadPool threadPool = Tasks.taskManager.pools.first().executor
        expect:
            assert mr.smallFinished && mr.bigFinished && mr.ratFinished
            assert Tasks.taskManager.pools.findAll { it.name.contains("MouseRace") }.size() == 1
            assert Tasks.taskManager.failed == 0
            assert threadPool.largestPoolSize > 0
            assert threadPool.largestPoolSize == MouseRace.pool
            assert threadPool.completedTaskCount == mr.processes().size()
            assert Tasks.taskManager.pools.first().executed > 1
        when:
            Tasks.exit()
        then:
            assert Tasks.taskManager.pools.first().executor.list.empty
    }
    /**
     * Test if tasks will last as long as the timeout
     * increasing the sleep time was causing stack overflow exception.
     * That was fixed in 2.7.4
     */
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

    def "If task is cancelled, it should not execute pending threads"() {
        setup :
            AtomicInteger times = new AtomicInteger()
            List<Runnable> runnables = []
            (1..6).each {
                int instance ->
                    runnables << {
                        println "[$instance] starting ..."
                        (1..10).each {
                            sleep(MILLIS_100)
                        }
                        println "[$instance] finished in "+times.incrementAndGet()+" place"
                    }
            }
            assert runnables.size() == 6
            ParallelTask parallelTask
            Tasks.runLater({
                parallelTask.cancel()
            }, "Later", MILLIS_300)
            parallelTask = ParallelTask.create(runnables, "Sleeping", 2, SECOND_10, Task.Priority.NORMAL, true)
            Tasks.add(parallelTask)
        when:
            sleep(MILLIS_200)
        then:
            assert parallelTask.cancelled
            assert times.get() < 4
        cleanup:
            Tasks.printOnScreen = true
            Tasks.printStatus()
    }
}