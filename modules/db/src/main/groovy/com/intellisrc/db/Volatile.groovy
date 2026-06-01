package com.intellisrc.db

import groovy.transform.CompileStatic

/**
 * This interface is for Databases which
 * can be stored in memory. In such cases
 * we need to keep the connection open or
 * the database will be destroyed.
 * @since 2026/05/28.
 */
@CompileStatic
interface Volatile {
    boolean isMemory()
}
