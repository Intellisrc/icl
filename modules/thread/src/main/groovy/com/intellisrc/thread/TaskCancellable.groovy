package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * This interface is for those task which allow to be canceled after execution
 * @since 2021/03/25.
 */
@CompileStatic
trait TaskCancellable {
    boolean cancelled = false
    void cancel() {
        this.cancelled = true
    }
    private void setCancelled(boolean cancel) {} //Prevent setting directly to force the use of cancel()
}