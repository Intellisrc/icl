package com.intellisrc.log

import groovy.transform.CompileStatic
import org.slf4j.event.Level

/**
 * @since 2021/08/03.
 */
@CompileStatic
trait LoggableOutputLevels implements Loggable {
    void initialize(Loggable baseLogger) {}
    List<Level> getLevels() {
        return Level.values().findAll { it <= level }.toList()
    }
    boolean hasLevel(Level level) {
        return getLevels().contains(level)
    }

    abstract Output getOutput()
}