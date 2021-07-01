package com.intellisrc.db.annot

import groovy.transform.CompileStatic

/**
 * @since 2021/06/29.
 */
@CompileStatic
enum DeleteActions {
    CASCADE, NULL, RESTRICT
}