package com.intellisrc.web.service

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

/**
 * Result of a match filter.
 * As parameters are extracted during filtering
 * they are included here
 */
@CompileStatic
@TupleConstructor
class MatchFilterResult {
    Optional<Service> route
    Map<String, String> params
}
