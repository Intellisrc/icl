package com.intellisrc.db

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @since 2026/05/27.
 */
@CompileStatic
class TableDefinition extends ConcurrentLinkedQueue<ColumnDefinition> {
    int version = 1

    boolean hasMultiplePk() {
        return this.count { it.primaryKey } > 1
    }
    List<ColumnDefinition> getPks() {
        return this.findAll { it.primaryKey }.toList()
    }
    Map<String, List<String>> getUniqueGroups() {
        Map<String, List<String>> groups = [:]

        this.each { ColumnDefinition col ->
            // Enforce safe boundaries against null, empty strings, or spaces
            if (col.uniqueGroup && !col.uniqueGroup.trim().empty) {
                groups.computeIfAbsent(col.uniqueGroup.trim(), { [] as List<String> }) << col.name
            }
        }

        return groups
    }
}
