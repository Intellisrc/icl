package com.intellisrc.thread

import com.intellisrc.core.Log
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/10.
 */
class ServiceTaskTest extends Specification {
    def setup() {
        Tasks.resetManager()
        Tasks.printOnChange = true
        Tasks.logToFile = false
    }

    class ServiceTest extends ServiceTask {
        int calledTimes = 0
        boolean running = true
        boolean exit = false
        boolean resetCalled = false
        boolean throwException = false
        boolean cancelCalled = false
        boolean pausedCalled = false
        boolean resumeCalled = false

        @Override
        Runnable process() throws InterruptedException {
            return {
                Log.i("Running...")
                while (!exit) {
                    if(running) {
                        calledTimes++
                        println "*****************[ CALLED $calledTimes ]**********************"
                        sleep(MILLIS_100)
                        if (throwException) {
                            throw new Exception("Man-made exception")
                        }
                    }
                }
                running = false
            }
        }

        @Override
        boolean reset() {
            Log.i("Got Reset")
            resetCalled = true

            calledTimes = 0
            running = true
            exit = false
            throwException = false
            cancelCalled = false
            pausedCalled = false
            resumeCalled = false
            return true
        }

        @Override
        void onCancel() {
            cancelCalled = true
            exit = true
        }

        @Override
        void onPause() {
            pausedCalled = true
            running = false
        }

        @Override
        void onResume() {
            resumeCalled = true
            running = true
        }
    }

    void assertServiceAndMonitor() {
        List procs = Tasks.findAll("ServiceTest")
        println "Running tasks: ----------------------"
        procs.each { println it.name }
        println "-----------------------------"
        assert procs.size() == 2 // Plus the monitor
        assert Tasks.get("ServiceTest").running
        assert Tasks.get("ServiceTest-monitor").running
    }

    def "Services should only run once"() {
        setup:
            ServiceTest st = new ServiceTest()
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
        when:
            sleep(HALF_SECOND)
        then:
            assert !Tasks.add(st): "Trying to add another one should fail"
        when:
            sleep(HALF_SECOND)
            assertServiceAndMonitor()
        then:
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes > 0
    }

    def "Services should recover from Exception"() {
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND) // Let it run
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes > 0
        when:
            called = st.calledTimes
            st.throwException = true
            sleep(HALF_SECOND)
        then:
            assert st.resetCalled
            assert st.calledTimes > 0
            assert called >= st.calledTimes
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 1
    }

    def "Services should recover from unexpected Exiting"() { // Not only exceptions
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND)
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes > 0
        when:
            called = st.calledTimes
            st.exit = true
            sleep(HALF_SECOND)
        then:
            assert st.resetCalled
            assert st.calledTimes > 0
            assert called >= st.calledTimes
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 1
    }

    // In case of cancel, we should not restart the service
    def "Services should not recover from Cancel"() {
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND) //Run service for some time
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes > 0
        when:
            called = st.calledTimes
            st.cancel()
            sleep(SECOND) //Simulate some extra time, to be sure it was cancelled correctly
        then:
            assert st.cancelled
            assert st.cancelCalled
            assert ! st.resetCalled
            assert ! st.running
            assert Math.abs(called - st.calledTimes) <= 1 //It might run once
        when:
            List procs = Tasks.findAll("ServiceTest")
            println "Running tasks: ----------------------"
            procs.each { println it.name }
            println "-----------------------------"
        then:
            assert procs.size() == 1
            assert Tasks.taskManager.failed == 0
    }

    def "Pause and resume should work"() {
        setup:
            int before
            int after
            int later
            ServiceTest st = new ServiceTest()
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND) //Run service for some time
        when:
            before = st.calledTimes
            st.pause()
            sleep(SECOND)
            after = st.calledTimes
        then:
            assert st.paused
            assert ! st.running
            assert before == after
        when:
            st.resume()
            sleep(SECOND)
            later = st.calledTimes
        then:
            assert ! st.paused
            assert st.running
            assert later > after
    }

    def "Tasks should be removed and stopped on command"() {
        setup:
            ServiceTest st = new ServiceTest()
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND)
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 0
        when:
            st.destroy()
            sleep(SECOND)
        then:
            assert st.cancelCalled
            assert !st.resetCalled
            assert !st.running
            assert Tasks.taskManager.failed == 0
            assert Tasks.findAll("ServiceTest").size() == 0
    }

}