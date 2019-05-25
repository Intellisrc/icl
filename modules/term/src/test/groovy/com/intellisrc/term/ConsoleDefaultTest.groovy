package com.intellisrc.term

import com.intellisrc.term.console.TestConsole

/**
 * @since 19/02/06.
 */
class ConsoleDefaultTest {
    static void main(String[] args) {
        Console.add(new TestConsole())
        Console.start()
    }
}