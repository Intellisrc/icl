package com.intellisrc.thread

import com.intellisrc.core.Log

import java.util.concurrent.atomic.AtomicInteger

import static com.intellisrc.core.Millis.*

/**
 * @since 2019/09/10.
 */
class ServiceTaskTest extends BaseTaskTest {
    class ServiceTest extends ServiceTask {
        AtomicInteger calledTimes = new AtomicInteger()
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
                        println "*****************[ CALLED ${calledTimes.incrementAndGet()} ]**********************"
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

            calledTimes.set(0)
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
            assert st.calledTimes.get() > 0
    }

    def "Services should recover from Exception"() {
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND) // Let it run
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes.get() > 0
        when:
            called = st.calledTimes.get()
            st.throwException = true
            sleep(HALF_SECOND)
        then:
            assert st.resetCalled
            assert st.calledTimes.get() > 0
            assert called >= st.calledTimes.get()
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
            assert st.calledTimes.get() > 0
        when:
            called = st.calledTimes.get()
            st.exit = true
            sleep(HALF_SECOND)
        then:
            assert st.resetCalled
            assert st.calledTimes.get() > 0
            assert called >= st.calledTimes.get()
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 1
    }

    // In case of cancel, we should not restart the service
    // And should not throw a warning (issue #62)
    def "Services should not recover from Cancel"() {
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(HALF_SECOND) //Run service for some time (it should run like 10 times in a second)
            assertServiceAndMonitor()
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes.get() > 0
        when:
            called = st.calledTimes.get()
            st.cancel()
            sleep(HALF_SECOND) //Simulate some extra time, to be sure it was cancelled correctly
        then:
            assert st.cancelled
            assert st.cancelCalled
            assert ! st.resetCalled
            assert ! st.running
            assert st.calledTimes.get() - called <= 2 //It might run once or twice
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
            sleep(HALF_SECOND) //Run service for some time (about 10 per second)
        when:
            before = st.calledTimes.get()
            st.pause()
            sleep(HALF_SECOND)
        then:
            assert st.pausedCalled
        when:
            after = st.calledTimes.get()
        then:
            assert st.paused
            assert ! st.running
            assert after - before <= 2 // It might have executed a few times after pause
        when:
            st.resume()
            sleep(HALF_SECOND)
        then:
            assert st.resumeCalled
        when:
            later = st.calledTimes.get()
        then:
            assert ! st.paused
            assert st.running
            assert later - after >= 4
    }

    def "Tasks should be removed and stopped on command"() {
        setup:
            ServiceTest st = new ServiceTest()
        expect:
            assert Tasks.add(st): "Adding the first one should be ok"
            sleep(SECOND)
            assertServiceAndMonitor()
            assert Tasks.get("ServiceTest").failed == 0
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