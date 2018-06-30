package com.intellisrc.tools

import groovy.transform.CompileStatic

/**
 * General commands to be used in practically all consoles
 * @since 18/06/29.
 */
@CompileStatic
class ConsoleDefault implements Consolable {

    @Override
    void onInit() {}

    @Override
    List<String> getAutoCompleteList() {
        return ["exit","quit","clear"]
    }

    @Override
    boolean onCommand(final LinkedList<String> commandLine) {
        if(commandLine) {
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

    @Override
    boolean onTimeOut() {
        Console.execCommand("exit")
        return true
    }

    @Override
    void onExit() {}
}
