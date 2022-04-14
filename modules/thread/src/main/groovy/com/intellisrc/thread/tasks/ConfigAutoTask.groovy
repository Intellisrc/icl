package com.intellisrc.thread.tasks

import com.intellisrc.core.Millis
import com.intellisrc.etc.config.ConfigAuto
import com.intellisrc.thread.IntervalTask
import groovy.transform.CompileStatic

import static com.intellisrc.core.Millis.*

/**
 * This Task is to update ConfigAuto from etc module.
 * @since 2021/07/01.
 */
@CompileStatic
class ConfigAutoTask extends IntervalTask {
    final ConfigAuto configAuto

    ConfigAutoTask(ConfigAuto configAuto, int sleepMillis = SECOND) {
        super([sleepMillis - MILLIS_100, MILLIS_100].max(), sleepMillis)
        this.configAuto = configAuto
    }

    @Override
    Runnable process() throws InterruptedException {
        return {
            configAuto.update()
        }
    }
}
