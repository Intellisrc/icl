# TERM Module (ICL.term)

Anything related to terminal is in this module (except AnsiColor, which is in `core`). 
It uses JLine to help you create interactive terminal (console) applications easily. 
It also contains some tools to display progress with colors.

[JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/modules/term/docs/)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/term)

## Classes and Interfaces

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

## TableMaker

Display data nicely in the terminal by creating tables using box drawing characters:

There are 4 ways to create a table:

### 1. Using List of List
```groovy
boolean header = true
boolean footer = false
new TableMaker([
    ["Fruit", "QTY", "Price", "Seller"],
    ["Apple","1000","10.00","some@example.com"],
    ["Banana","2002","15.00","anyone@example.com"],
    ["Mango","400","134.10","dummy200@example.com"],
    ["Kiwi","900","2350.40","example@example.com"]
], header, footer).print()
```

### 2. Using List of Map
```groovy
boolean footer = false
new TableMaker([
    [
        Fruit   : "Apple",
        QTY     : "1000",
        Price   : "10.00",
        Seller  : "some@example.com"
    ],
    [
        Fruit   : "Banana",
        QTY     : "2002",
        Price   : "15.00",
        Seller  : "anyone@example.com"
    ]
], footer).print()
```

### 3. Specifying parameters (Groovy style)
```groovy
TableMaker table = new TableMaker(
    headers : ["Fruit", "QTY", "Price", "Seller"],
    footer  : ["Fruits: 4", "Sum: 4302", "Total: 2509.5", ""],
    compact : true
)
data.each {
    table << [data.fruit, data.qty.toString(), String.format("%.2f", data.price), data.email]
}
table.print()
```

### 4. Using methods

```groovy
TableMaker table = new TableMaker()
table.compact = true
table.setHeaders(["Fruit", "QTY", "Price", "Seller"])
table.addRow(["Apple","1000","10.00","some@example.com"])
table.addRow(["Mango","400","134.10","dummy200@example.com"])
table.addFooter("Have a healthy salad today!")
table.print()
```

### Styling

There are some options you can set to make your tables look the way you want:

#### compact = true

When enabled, it won't draw lines each row, for example:

```groovy
// You can set it in the constructor
TableMaker table = new TableMaker(compact: true)
// Or later on
table.compact = true
// Or during print
table.print(true)
```

Example with `compact=false` (default):

```
 +-------------------+--------------------------------+-----+
 | Name              | Email                          | Age |
 |-------------------+--------------------------------+-----|
 | Joshep Patrishius | jp@example.com                 | 41  |
 |-------------------+--------------------------------+-----|
 | Zoe Mendoza       | you-know-who@example.com       | 54  |
 +-------------------+--------------------------------+-----+
```

Example with `compact=true`:

```
 +-------------------+--------------------------------+-----+
 | Name              | Email                          | Age |
 |-------------------+--------------------------------+-----|
 | Joshep Patrishius | jp@example.com                 | 41  |
 | Zoe Mendoza       | you-know-who@example.com       | 54  |
 +-------------------+--------------------------------+-----+
```

#### Border styles

There are several styles to choose from in the package : `term.styles`

By default, tables are rendered with the `SafeStyle` class (which will render 
correctly in any terminal or monospace font).

You can choose a different style or create your own. To set it:
```groovy
// You can set it in the constructor
TableMaker table = new TableMaker(style: new ClassicStyle())
// Or later on
table.style = new DoubleLineStyle()
// Or during print
table.print(new SemiDoubleStyle(), /* compact */ true)
table.print(/* compact */ true, new SemiDoubleStyle())
table.print(new SemiDoubleStyle())
```

The available styles are (additionally to the `SafeStyle` shown before:

* ClassicStyle

```
┌───────────┬───────────┬───────────────┬──────────────────────┐
│ Fruit     │ QTY       │ Price         │ Seller               │
├───────────┼───────────┼───────────────┼──────────────────────┤
│ Apple     │ 1000      │ 10.00         │ some@example.com     │
├───────────┼───────────┼───────────────┼──────────────────────┤
│ Fruits: 4 │ Sum: 4302 │ Total: 2509.5 │                      │
└───────────┴───────────┴───────────────┴──────────────────────┘
```

* DoubleLineStyle

```
 ╔═══════════╦═══════════╦═══════════════╦══════════════════════╗
 ║ Apple     ║ 1000      ║ 10.00         ║ some@example.com     ║
 ╠═══════════╬═══════════╬═══════════════╬══════════════════════╣
 ║ Fruits: 4 ║ Sum: 4302 ║ Total: 2509.5 ║                      ║
 ╚═══════════╩═══════════╩═══════════════╩══════════════════════╝
```

* SemiDoubleStyle

```
╔════════╤══════╤═════════╤══════════════════════╗
║ Fruit  │ QTY  │ Price   │ Seller               ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Apple  │ 1000 │ 10.00   │ some@example.com     ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Kiwi   │ 900  │ 2350.40 │ example@example.com  ║
╚════════╧══════╧═════════╧══════════════════════╝
```

* BoldStyle

```
 ┏━━━━━━━━┳━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━┓
 ┃ Fruit  ┃ QTY  ┃ Price   ┃ Seller               ┃
 ┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
 ┃ Kiwi   ┃ 900  ┃ 2350.40 ┃ example@example.com  ┃
 ┗━━━━━━━━┻━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━┛
```

* ThinStyle

```
┌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┐
┊ Apple  ┊ 1000 ┊ 10.00   ┊ some@example.com     ┊
├╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┤
┊ Kiwi   ┊ 900  ┊ 2350.40 ┊ example@example.com  ┊
└╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┘
```

To create your own style, you can either extend any of the previous styles, 
extend the abstract class `BasicStyle` or implement `Stylable` interface.

#### Customizing columns

You can further customize columns or how data is displayed by accessing
the `table.columns` before printing:

```groovy
table.colums[2].align = RIGHT
// More properties will be added in later versions
```

Instead of adding a row with `addRows` you can add them with `addCells` with 
a custom formatter:

```groovy
table.addCells([
    // Using AnsiColor from `core` module:
    new Cell(status, { (it == "OK" ? GREEN : RED) + status + RESET }),
    new Cell(host, { BLUE + it + RESET }),
    /* ... */
])
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
import com.intellisrc.term.Console 

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
        // 'quit' and 'exit' are implemented by default, so we skip them here:
        if (!commandList.empty && !["quit", "exit"].contains(commandList.first())) {
            if(!commandList.first().empty) {
                String cmd = commandList.poll()
                switch(cmd) {
                    case "test":
                        String what = commandList.empty ? "ok" : commandList.poll()
                        Console.resetPreviousPrompt(what + "! ")
                        break
                    case "testing":
                        Console.read("Testing...", {
                            (1..30).each {
                                sleep(100)
                                print "."
                            }
                            Console.cancel()
                            Console.out("")
                            Console.resetRead()
                        } as Console.BackgroundTask)                    
                        break
                }
            }
        }
        // In this example, only 'test' and 'testing' are implemented
        // In order to allow multiple 'Consolable' implementations
        // we return 'true', otherwise it will not continue.
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
        // Console.out works like `Log` in `core` module.
        Console.out("%s Bye! %s", AnsiColor.RED, "Bye!", AnsiColor.RESET)
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
