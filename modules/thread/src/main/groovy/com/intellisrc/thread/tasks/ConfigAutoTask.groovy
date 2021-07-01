package com.intellisrc.thread.tasks

import com.intellisrc.etc.config.ConfigAuto
import com.intellisrc.thread.IntervalTask
import groovy.transform.CompileStatic

/**
 * This Task is to update ConfigAuto from etc module.
 * @since 2021/07/01.
 */
@CompileStatic
class ConfigAutoTask extends IntervalTask {
    final ConfigAuto configAuto

    ConfigAutoTask(ConfigAuto configAuto, long maxExecutionMillis = 900, int sleepMillis = 1000) {
        super(maxExecutionMillis, sleepMillis)
        this.configAuto = configAuto
    }

    @Override
    Runnable process() throws InterruptedException {
        return {
            configAuto.update()
        }
    }
}
