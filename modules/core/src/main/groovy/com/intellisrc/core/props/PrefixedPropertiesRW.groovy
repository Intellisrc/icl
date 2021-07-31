package com.intellisrc.core.props

import groovy.transform.CompileStatic

/**
 * @since 2021/07/26.
 */
@CompileStatic
trait PrefixedPropertiesRW implements PropertiesRW, PrefixedProperties {}
