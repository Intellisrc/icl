package com.intellisrc.tools

import groovy.transform.CompileStatic
import org.jline.reader.Completer

/**
 * @since 18/06/29.
 */
@CompileStatic
trait Consolable {
    /**
     * Code to execute on initialization
     * @param arguments : command line arguments or options
     * @return initial command if any
     */
    abstract void onInit(LinkedList<String> arguments)
    /**
     * Get the list of words to use as auto-complete
     * @return
     */
    abstract List<String> getAutoCompleteList()
    /**
     * Get completer (optional)
     * @link https://github.com/jline/jline3/wiki/Completion
     *
     * Implementations:
     * -----------------------------------------------------------------------------------------------
     * ArgumentCompleter : Each completer will be used to complete the n-th word on the command line.
     * FileNameCompleter : returns matching paths (directories or files)
     * DirectoriesCompleter : returns matching directories
     * FilesCompleter : returns matching files
     * EnumCompleter : returns a list of candidates based on an Enum names
     * StringsCompleter : returns a list of candidates based on a static list of strings (use getAutoCompleteList)
     * RegexCompleter : delegates to several other completers depending on a given regular expression.
     * TreeCompleter : completes commands based on a tree structure
     * MatchAnyCompleter : returns a list of candidates based in any of the words
     * -----------------------------------------------------------------------------------------------
     *
     * @return Completer interface
     */
    Completer getCompleter() { null }
    /**
     * Do something on command.
     * @param commandLine : List of arguments in a single line
     * @return true to continue with next Consolable
     */
    abstract boolean onCommand(final LinkedList<String> commandLine)
    /**
     * Do something on timeout. (optional)
     * console.timeout configuration rule must be set greater than 0 to work
     * @return true to continue with next Consolable
     */
    boolean onTimeOut() { true }
    /**
     * Code to execute on finish
     */
    abstract void onExit()
}