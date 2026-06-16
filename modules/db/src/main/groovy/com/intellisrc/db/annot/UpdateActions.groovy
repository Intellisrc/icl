package com.intellisrc.db.annot

import groovy.transform.CompileStatic

/**
 * @since 2021/06/29.
 */
@CompileStatic
enum UpdateActions {
    NO_ACTION, CASCADE //Although RESTRICT exists, in most databases is the same as "NO_ACTION", but NO_ACTION is safer.
    @Override
    String toString() {
        return super.toString().replace("_", " ")
    }
}