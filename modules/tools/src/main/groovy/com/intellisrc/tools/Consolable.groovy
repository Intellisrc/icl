package com.intellisrc.tools

import groovy.transform.CompileStatic

/**
 * @since 18/06/29.
 */
@CompileStatic
interface Consolable {
    /**
     * Code to execute on initialization
     */
    void onInit()
    /**
     * Get the list of words to use as auto-complete
     * @return
     */
    List<String> getAutoCompleteList()
    /**
     * Do something on command.
     * @param commandLine : List of arguments in a single line
     * @return true to continue with next Consolable
     */
    boolean onCommand(final LinkedList<String> commandLine)
    /**
     * Do something on timeout.
     * console.timeout configuration rule must be set greater than 0 to work
     * @return true to continue with next Consolable
     */
    boolean onTimeOut()
    /**
     * Code to execute on finish
     */
    void onExit()
}