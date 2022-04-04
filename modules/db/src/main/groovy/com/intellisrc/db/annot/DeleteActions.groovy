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
        String str = super.toString()
        switch (this) {
            case NULL: str = "SET " + super.toString(); break
        }
        return str
    }
}