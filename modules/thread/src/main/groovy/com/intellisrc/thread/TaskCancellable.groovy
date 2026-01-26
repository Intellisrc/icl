package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * This interface is for those task which allow to be canceled after execution
 * A cancelled task will be interrupted and can't be executed again (unless it is destroyed)
 * such tasks will remain on the list, use destroy() to remove it.
 * Use pause() to stop some task and resume() to continue if needed.
 * @since 2021/03/25.
 */
@CompileStatic
trait TaskCancellable {
    private boolean cancelled = false

    final void cancel() {
        this.cancelled = true
    }

    boolean isCancelled() {
        return cancelled
    }

    // To Override
    void onCancel() {}
}