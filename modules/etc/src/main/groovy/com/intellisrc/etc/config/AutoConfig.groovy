package com.intellisrc.etc.config

import groovy.transform.CompileStatic

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Enable "Auto Configuration" for classes or fields
 * You set the annotation in those fields that you want to store and control, for example:
 *
 * @AutoConfig(key = "web")
 * class WebConfig {
 *      @AutoConfig(key = "service.port", userFriendly = true)    // Value will be stored in web.service.port and users can modify it
 *      static int port = 80
 * }
 *
 * @since 2021/03/12.
 */
@CompileStatic
@Target([
    ElementType.TYPE,
    ElementType.FIELD
])
@Retention(RetentionPolicy.RUNTIME)
@interface AutoConfig {
    String root() default "config" // This serves as a filter so only those marked with the same root will processed together
    String key() default ""
    boolean export() default true // If a key should be exported into the configuration file or not (false = internal use only)
    boolean userFriendly() default false // Specify which of the fields can be modified by the end user (false = developer only)
}