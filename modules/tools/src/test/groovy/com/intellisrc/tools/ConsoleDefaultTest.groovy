package com.intellisrc.tools

import com.intellisrc.tools.console.TestConsole

/**
 * @since 19/02/06.
 */
class ConsoleDefaultTest {
    static void main(String[] args) {
        Console.add(new TestConsole())
        Console.start()
    }
}