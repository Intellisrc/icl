package com.intellisrc.thread

import groovy.transform.CompileStatic

/**
 * @since 2019/09/17.
 */
@CompileStatic
interface Killable {
    abstract void kill()
}