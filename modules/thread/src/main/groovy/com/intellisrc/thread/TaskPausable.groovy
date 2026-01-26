package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * This interface is for those task which allow to be paused
 * @since 2021/03/25.
 */
@CompileStatic
trait TaskPausable {
    private boolean paused = false

    final void pause() {
        this.paused = true
        onPause()
    }
    final void resume() {
        this.paused = false
        onResume()
    }
    boolean isPaused() {
        return paused
    }
    // To override
    void onPause() {}
    void onResume() {}
}