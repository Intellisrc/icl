package com.intellisrc.db.auto

import com.intellisrc.db.DB
import groovy.transform.CompileStatic

import java.lang.annotation.Annotation

/**
 * @since 2022/07/05.
 * Methods used by TableUpdater
 */
@CompileStatic
trait AutoJDBC {
    /**
     * AutoJDBC uses CreateTable so we need to store annotation
     */
    Annotation meta
    /**
     * Overwrite for those databases which require some kind of
     * initialization
     * @param db
     * @return
     */
    boolean initialize() {
        return true
    }
}