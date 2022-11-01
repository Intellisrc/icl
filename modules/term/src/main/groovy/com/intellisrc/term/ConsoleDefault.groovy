package com.intellisrc.term

import groovy.transform.CompileStatic

/**
 * General commands to be used in practically all consoles
 * @since 18/06/29.
 */
@CompileStatic
class ConsoleDefault implements Consolable {

    @Override
    void onInit(LinkedList<String> args) {}

    /**
     * Basic commands to auto-complete
     * @return
     */
    @Override
    List<String> getAutoCompleteList() {
        return ["exit","quit","clear"]
    }

    /**
     * Process basic commands
     * @param commandLine
     * @return true, which means it will continue to next available Console (controlled in 'Console')
     */
    @Override
    boolean onCommand(final LinkedList<String> commandLine) {
        if(commandLine) {
            //noinspection GroovyFallthrough
            switch (commandLine.poll()) {
                case "clear":
                    Console.clearScreen()
                    break
                case "exit":
                case "quit":
                    Console.clearScreen()
                    Console.exit()
                    break
            }
        }
        return true
    }

    /**
     * In case console.timeout setting is set, it will exit automatically
     * @return
     */
    @Override
    boolean onTimeOut() {
        Console.execCommand("exit")
        return true
    }

    @Override
    void onExit() {}
}
