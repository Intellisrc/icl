package com.intellisrc.core

import com.intellisrc.etc.Hardware
import spock.lang.Specification

class ConfigEtcTest extends Specification {
    def "Reading multiple times a value in config.properties should not exhaust resources"() {
        setup:
            double initial = 0
            double last = 0
        when:
            List descs = []
            (0..1000). each {
                descs << Config.any.get("log.level", "verbose")
                Hardware.getOpenFiles({
                    double total ->
                        if(!initial) {
                            initial = total
                        } else {
                            last = total
                        }
                        println String.format("Open Files: %.0f", total)
                })
            }
        then:
            assert last - initial < 10
    }
}
