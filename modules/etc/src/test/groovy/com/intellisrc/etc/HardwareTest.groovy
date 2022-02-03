package com.intellisrc.etc

import com.intellisrc.core.Log
import spock.lang.Ignore
import spock.lang.Specification

/**
 * @since 2019/10/08.
 */
@Ignore //Some features may not be available in all testing environments
class HardwareTest extends Specification {
    def "Checking monitor"() {
        setup:
            boolean isOn = Hardware.screenOn
            if(!isOn) {
                Hardware.screenOn = true
                sleep(100)
            }
        expect:
            assert Hardware.screenOn : "The screen should start ON"
        when:
            Hardware.screenOn = false
            sleep(100)
        then:
            assert ! Hardware.screenOn : "The screen should be OFF"
        cleanup:
            Hardware.screenOn = true
    }
    
    def "Enabling / Disabling input"() {
        when:
            Hardware.disableInputDevice()
        then:
            notThrown Exception
        cleanup:
            Hardware.enableInputDevice()
    }
    
    def "Enabling / Disabling Keyboard"() {
        when:
            Hardware.disableInputDevice("keyboard")
        then:
            notThrown Exception
        cleanup:
            //sleep(3000) // To test, uncomment and toggle num lock
            Hardware.enableInputDevice("keyboard")
    }
    
    def "Enabling / Disabling Mouse"() {
        when:
            Hardware.disableInputDevice("pointer")
        then:
            notThrown Exception
        cleanup:
            //sleep(3000) // To test, uncomment and move mouse
            Hardware.enableInputDevice("pointer")
    }
    
    def "Get Runtime memory"() {
        setup:
            Hardware.debug = true
        expect:
            Hardware.getRuntimeMemoryUsage({
                double value ->
                    assert value
                    Log.i("Value: %.2f", value)
            })
        
    }
}