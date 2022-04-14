package com.intellisrc.db.annot

import groovy.transform.CompileStatic

/**
 * @since 2021/06/29.
 */
@CompileStatic
enum DeleteActions {
    CASCADE, NULL, RESTRICT
    @Override
    String toString() {
        return (this == NULL ? "SET " : "") + super.toString()
    }
}