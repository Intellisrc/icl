package com.intellisrc.thread

import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import spock.lang.Specification

import static com.intellisrc.core.Millis.*
import static com.intellisrc.core.Millis.HALF_SECOND
import static com.intellisrc.core.Millis.HALF_SECOND
import static com.intellisrc.core.Millis.HALF_SECOND
import static com.intellisrc.core.Millis.HALF_SECOND


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
        boolean running = true
        int calledTimes = 0
        boolean resetCalled = false
        boolean throwException = false
        boolean exit = false
        @Override
        Runnable process() throws InterruptedException {
            boolean goOut = false
            return {
                Log.i("Running...")
                while(running &&! goOut) {
                    calledTimes++
                    println "*****************[ CALLED $calledTimes ]**********************"
                    sleep(MILLIS_100)
                    if(throwException) {
                        throwException = false
                        if(exit) {
                            goOut = true
                        } else {
                            throw new Exception("Man-made exception")
                        }
                    }
                }
            }
        }
    
        @Override
        boolean reset() {
            Log.i("Got Reset")
            resetCalled = true
            running = true
            calledTimes = 0
            return true
        }
    }
    
    def "Services should only run once"() {
        setup:
            ServiceTest st = new ServiceTest()
        expect:
            assert Tasks.add(st) : "Adding the first one should be ok"
        when:
            sleep(HALF_SECOND)
        then:
            assert !Tasks.add(st) : "Trying to add another one should fail"
        when:
            sleep(HALF_SECOND)
        then:
            assert Tasks.taskManager.pools.findAll { it.name.contains("ServiceTest") }.size() == 2 //Plus the monitor
            assert Tasks.taskManager.failed == 0
            assert st.calledTimes > 0
    }
    
    def "Services should recover from Exception"() {
        setup:
            ServiceTest st = new ServiceTest()
            int called
        expect:
            assert Tasks.add(st) : "Adding the first one should be ok"
            sleep(SECOND)
            assert Tasks.taskManager.pools.findAll { it.name.contains("ServiceTest") }.size() == 2 // Plus the monitor
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
            assert Tasks.taskManager.pools.findAll { it.name.contains("ServiceTest") }.size() == 2 // Plus the monitor
            assert Tasks.taskManager.failed == 1
    }
    
    def "Services should recover from Exiting"() {
        setup:
            ServiceTest st = new ServiceTest()
            st.exit = true
            int called
        expect:
            assert Tasks.add(st) : "Adding the first one should be ok"
            sleep(SECOND)
            assert Tasks.taskManager.pools.findAll { it.name.contains("ServiceTest") }.size() == 2 // Plus the monitor
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
            assert Tasks.taskManager.pools.findAll { it.name.contains("ServiceTest") }.size() == 2 // Plus the monitor
            assert Tasks.taskManager.failed == 1
    }
}