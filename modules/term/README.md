# TERM Module

Anything related to terminal is in this module (except AnsiColor, which is in `core`). 
It uses JLine to help you create interactive terminal (console) applications easily. 
It also contains some tools to display progress with colors.

* `Consolable` : Interface to implement for any console application
* `Console` : Main console class which uses `Consolable` applications. It wraps jLine
* `ConsoleDefault` : Simple implementation of `Consolable` which provides common commands
* `Progress` : Show a progress bar in a terminal (it has multiple implementations and options)

## Progress

It provides two easy ways to visualize progress on a terminal:

### Example
```groovy
(1..100).each {
    Progress.summary(it, 100, "Summary: ")
    Progress.bar(it, 100, "Progress: ", charsWidth)
}
```

## Console

This class is a wrapper around `jLine` with the idea of making it easier
to deploy terminal applications. These applications are useful when UI is
not possible or desired, or you just want an easy way to interact with 
your system and data (in other words, a "Command Line Client").

Other goal of this class is to provide ways to "plug-in" features that
can be reused in other console applications (those classes should extend
`Consolable` interface).

### Example

```groovy
class CustomConsole implements Consolable {

    // Code to execute on initialization
    void onInit(LinkedList<String> arguments) {
        Console.prompt = "test \$"  // Change what to display as prompt
        Console.timeout = 10        // seconds to timeout for input
    }
    // Get the list of words to use as auto-complete
    List<String> getAutoCompleteList() {
        return [ "test", "testing", "tester", "tested", "testy" ]
    }

    // Do something on command
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

    // What to do on timeout
    boolean onTimeOut() {
        Console.out("Timeout")
        Console.resetRead()
        return false //do not continue
    }
    
    // What to do on exit
    void onExit() {
        Console.out(AnsiColor.RED + "Bye!" + AnsiColor.RESET)
    }
}

// Somewhere in your code...
Console.add(new CustomConsole()) //You can add as many Consolable classes
Console.start()
```

There are more advanced uses for this class, check the full documentation.
Specially you can use `Completer` [classes supported by jLine](https://github.com/jline/jline3/wiki/Completion).
And remember that you can use `AnsiColor` in `core` module to give colors to
your application.
