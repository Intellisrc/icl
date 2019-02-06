package com.intellisrc.tools.console

import com.intellisrc.tools.Consolable
import com.intellisrc.tools.Console
import groovy.transform.CompileStatic

/**
 * @since 19/02/06.
 */
@CompileStatic
class TestConsole implements Consolable {

    @Override
    void onInit(LinkedList<String> arguments) {
        Console.prompt = "test \$"
        Console.timeout = 10
    }

    @Override
    List<String> getAutoCompleteList() {
        return [ "test", "testing", "tester", "tested", "testy" ]
    }

    @Override
    boolean onCommand(LinkedList<String> commandList) {
        if (!commandList.empty && !["quit", "exit"].contains(commandList.first())) {
            if(!commandList.first().empty) {
                Console.resetPreviousPrompt("ok!")
                Console.read("Reading...", {
                    (1..30).each {
                        sleep(100)
                        print "."
                    }
                    Console.cancel()
                    Console.out("")
                    Console.resetRead()
                } as Console.BackgroundTask)
            }
        }
        return true
    }

    @Override
    boolean onTimeOut() {
        Console.out("Timeout")
        Console.resetRead()
        return false //do not continue
    }

    @Override
    void onExit() {

    }
}
