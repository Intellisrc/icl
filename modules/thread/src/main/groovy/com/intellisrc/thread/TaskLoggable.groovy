package com.intellisrc.thread

import groovy.transform.CompileStatic

import java.time.LocalDateTime

/**
 * Information to export to log about tasks and pools
 * @since 2019/09/04.
 */
@CompileStatic
interface TaskLoggable {
    String getName()
    String getFullName()
    String getIndicator() //Character to print about type
    String getStatus()
    LocalDateTime getSetupTime()
    LocalDateTime getStartTime()
    LocalDateTime getWaitTime()
    LocalDateTime getDoneTime()
    LocalDateTime getFailTime()
    int getSleep()
    int getMaxExec()
    int getFailed()
    int getExecuted()
    boolean isRunning()
}