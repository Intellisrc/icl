package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification

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
                            sleep(10)
                        }
                        print "[s]"
                        smallFinished = true
                    },
                    {
                        //Big and slow
                        (1..20).each {
                            print "B"
                            sleep(30)
                        }
                        print "[B]"
                        bigFinished = true
                    },
                    {
                        //Ratatuille mouse
                        (1..20).each {
                            print "r"
                            sleep(20)
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
    
    // TODO: def "Creating ParallelTask inline"() {}
}