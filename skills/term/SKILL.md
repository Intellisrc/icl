---
name: term
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for terminal apps - interactive consoles with command history and tab-completion (Console, Consolable, ConsoleDefault on JLine), colored output helpers, progress bars (Progress), pretty terminal tables with styles and column formatting (TableMaker), and command completers (MatchAnyCompleter). Triggers on com.intellisrc.term imports in an ICL project. Includes core.
---

# ICL term Module

Interactive terminal apps for ICL, wrapping JLine. Includes core (colors live in `core.AnsiColor`).

```groovy
implementation 'com.intellisrc:term:2.10.5'   // + core + groovy
```

Package: `com.intellisrc.term` (styles in `term.styles`).

## Console — interactive shell

Implement `Consolable` and hand it to the static `Console`:

```groovy
import com.intellisrc.term.*

class MyApp implements Consolable {
    @Override void onInit(LinkedList<String> args) { Console.prompt = "myapp> " }
    @Override List<String> getAutoCompleteList() { ["hello", "goodbye", "users"] }
    @Override boolean onCommand(LinkedList<String> cmd) {
        if (cmd) switch (cmd.poll()) {
            case "hello":  Console.success("Hello %s!", cmd ? cmd.poll() : "world") ; break
            case "users":  showUsers() ; break
            case "goodbye": return false        // stop processing; false also ends the loop
        }
        return true                             // true = continue to next Consolable
    }
    @Override void onExit() { Console.out("bye") }
    // optional: Completer getCompleter() { new MatchAnyCompleter(["alpha","beta"]) }
    // optional: boolean onTimeOut() { ... }    // needs Console.timeout > 0 (seconds)
}

Console.add(new MyApp())
Console.start()                                 // Console.start(args) passes them to onInit
```

Static helpers: `Console.out/warn/error/success/info(msg, args...)`, `Console.read(prompt[, backgroundTask])`, `Console.readPassword(mask)`, `Console.clearScreen(msg)`, `Console.resetRead(msg)`, `Console.exit(code)`.

`ConsoleDefault` is auto-added (`Console.addDefault = true`) and provides `exit`, `quit` and `clear`. Settings: `Console.prompt` (default `"> "`), `Console.mask` (`'*'`), `Console.timeout` (seconds, 0 = off).

Background task while waiting for input (progress/status next to the prompt):

```groovy
Console.read("working...", {
    int getDelay()  { 0 }
    int getPeriod() { 1 }
    boolean call()  { updateStatus() ; true }   // false stops it
} as Console.BackgroundTask)
```

## Progress — progress display

```groovy
int total = 100
(1..total).each {
    Progress.bar(it, total, "Processing:", 50)   // colored bar: green/yellow/red by percentage
    // Progress.summary(it, total, "Processing:") // text-only: 5/100 : 5.00%
    sleep(10)
}
println()                                       // REQUIRED: methods use \r to redraw one line
```

## TableMaker — terminal tables

```groovy
new TableMaker(
    headers: ["Name", "Age", "Email"],
    footer: ["Average:", avg, ""],
    compact: true,                               // no row separators
    style: new ClassicStyle(),                   // default SafeStyle (ASCII)
    borderColor: AnsiColor.CYAN
).with {
    addRow(["Alice", 30, "a@x.com"])
    addRow(["Bob", 25, "b@x.com"])
    print()                                      // also print(compact) / print(style, compact)
}

// From data directly:
new TableMaker([name: "Alice", age: 30], true).print()                    // Map (horizontal)
new TableMaker(rowsListOfMaps, false).print()                             // List<Map>
new TableMaker([[ "Name", "Age" ], ["Alice", 30]], true, false).print()   // List<List> + header
// also setHeaders(...), setRows(...), setFooter(...), << row (leftShift)
```

Styles (`term.styles`): `SafeStyle` (ASCII, default), `ClassicStyle`, `DoubleLineStyle`, `SemiDoubleStyle`, `BoldStyle`, `ThinStyle`.

Column customization — formatters, colors, alignment:

```groovy
TableMaker table = new TableMaker(headers: ["Price", "Stock"])
table.columns[0].with {
    align = TableMaker.Align.RIGHT               // LEFT, RIGHT, CENTER
    formatter = { String.format("$ %.2f", it as double) }
}
table.columns[1].color = { Cell c -> c.value > 100 ? AnsiColor.GREEN : AnsiColor.RED }
table.columns[1].maxLen = 10                     // truncate; ellipsis = true
// Cell exposes: value, row, col, table (use table.getCell(row, col) for cross-column rules)
```

## MatchAnyCompleter

JLine `Completer` matching candidates containing all typed words (substring, case-sensitive). Return it from `Consolable.getCompleter()`; `Console` merges it with `getAutoCompleteList()`.

## Gotchas

- `onCommand()` receives words as `LinkedList<String>` (already tokenized); poll them in order.
- Handle unknown commands: return `true` to let other `Consolable`s (like ConsoleDefault) see them.
- Progress output MUST be followed by `println()` or the next log line overwrites it.
- Prompt and other static fields must be set in `onInit()`, before the read loop starts.
- ANSI features (colors, box styles) degrade on dumb terminals — prefer `SafeStyle` for logs/CI.
