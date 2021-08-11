package com.intellisrc.log

import com.intellisrc.core.Config
import groovy.transform.CompileStatic
import org.slf4j.event.Level

import static com.intellisrc.log.Output.OutputType.CACHED_SYS_OUT
import static com.intellisrc.log.Output.OutputType.SYS_OUT

/**
 * This logger will output in standard out
 * @since 2021/08/02.
 */
@CompileStatic
class PrintLogger extends BaseLogger implements LoggableOutputLevels {
    protected boolean initialized = false
    protected Output output
    protected boolean split = false
    protected boolean cache = false

    PrintLogger() {
        enabled = Config.get("log.print", true)
    }
    /**
     * Package access allows only {@link CommonLoggerFactory} to instantiate
     * CommonLogger instances.
     */
    @Override
    void initialize(Loggable baseLogger) {
        if(initialized) { return }
        initialized = true
        // Print settings:
        if(enabled) {
            level             = getLevelFromString(Config.get("log.print.level", baseLogger.level.toString()))
            useColor          = Config.get("log.print.color", baseLogger.useColor)
            colorInvert       = Config.get("log.print.color.invert", baseLogger.colorInvert)
            showDateTime      = Config.get("log.print.show.time", baseLogger.showDateTime)
            showThreadName    = Config.get("log.print.show.thread", baseLogger.showThreadName)
            showThreadShort   = Config.get("log.print.show.thread.short", baseLogger.showThreadShort)
            showLogName       = Config.get("log.print.show.logger", baseLogger.showLogName)
            levelInBrackets   = Config.get("log.print.show.level.brackets", baseLogger.levelInBrackets)
            levelAbbreviated  = Config.get("log.print.show.level.short", baseLogger.levelAbbreviated)
            showPackage       = Config.get("log.print.show.package", baseLogger.showPackage)
            showClassName     = Config.get("log.print.show.class", baseLogger.showClassName)
            showMethod        = Config.get("log.print.show.method", baseLogger.showMethod)
            showLineNumber    = Config.get("log.print.show.line.number", baseLogger.showLineNumber)
            dateFormatter     = Config.get("log.print.show.time.format", baseLogger.dateFormatter)
            showThreadHead    = Config.get("log.print.show.thread.head", baseLogger.showThreadHead)
            showThreadTail    = Config.get("log.print.show.thread.tail", baseLogger.showThreadTail)
            showStackTrace    = Config.get("log.print.show.stack", baseLogger.showStackTrace)
            ignoreList        = Config.get("log.print.ignore", baseLogger.ignoreList)
            ignoreLevel       = Config.get("log.print.ignore.level", baseLogger.ignoreLevel) as Level

            // Split SysOut and SysErr
            split = Config.get("log.print.split", false)
            // Use cache_out and cache_err
            cache = Config.get("log.print.cache", false)
            // Set output:
            output = new Output(cache ? CACHED_SYS_OUT : SYS_OUT)
        }
    }

    @Override
    Output getOutput() {
        return this.output
    }

    @Override
    List<Level> getLevels() {
        return Level.values().findAll {
            split ? (it > Level.WARN && it <= level) : (it <= level)
        }.toList()
    }
}
