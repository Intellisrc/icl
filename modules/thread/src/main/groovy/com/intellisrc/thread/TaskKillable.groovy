package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * This class is for those Tasks that are expected to be killed (check `isKilled()`) as a way to interrupt their process
 * NOTE: There is no kill() method (as cancel() in TaskCancellable) because it is not designed to be called on command,
 * that is what cancel() is for. TaskKillable is for the ThreadPoll to kill the process when needed by sending an interrupt signal
 * (for example, during timeouts). The main difference is that cancel() will not relaunch the process again.
 * @since 2019/09/17.
 */
@CompileStatic
trait TaskKillable {
    abstract void onKill()

    boolean isKilled() {
        return Thread.currentThread().isInterrupted()
    }
}