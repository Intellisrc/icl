package com.intellisrc.log

import com.intellisrc.core.Config
import groovy.transform.CompileStatic
import org.slf4j.event.Level

import static com.intellisrc.log.Output.OutputType.*

/**
 * This logger will output in standard err
 * @since 2021/08/02.
 */
@CompileStatic
class PrintStdErrLogger extends PrintLogger {
    /**
     * Package access allows only {@link CommonLoggerFactory} to instantiate
     * CommonLogger instances.
     */
    @Override
    void initialize(Loggable baseLogger) {
        if(initialized) { return }
        super.initialize(baseLogger)
        // Print settings:
        if(enabled) {
            level             = getLevelFromString(Config.get("log.print.stderr.level", baseLogger.level.toString()))
            useColor          = Config.get("log.print.stderr.color", baseLogger.useColor)
            colorInvert       = Config.get("log.print.stderr.color.invert", baseLogger.colorInvert)
            showDateTime      = Config.get("log.print.stderr.show.time", baseLogger.showDateTime)
            showThreadName    = Config.get("log.print.stderr.show.thread", baseLogger.showThreadName)
            showThreadShort   = Config.get("log.print.stderr.show.thread.short", baseLogger.showThreadShort)
            showLogName       = Config.get("log.print.stderr.show.logger", baseLogger.showLogName)
            levelInBrackets   = Config.get("log.print.stderr.show.level.brackets", baseLogger.levelInBrackets)
            levelAbbreviated  = Config.get("log.print.stderr.show.level.short", baseLogger.levelAbbreviated)
            showPackage       = Config.get("log.print.stderr.show.package", baseLogger.showPackage)
            showClassName     = Config.get("log.print.stderr.show.class", baseLogger.showClassName)
            showMethod        = Config.get("log.print.stderr.show.method", baseLogger.showMethod)
            showLineNumber    = Config.get("log.print.stderr.show.line.number", baseLogger.showLineNumber)
            dateFormatter     = Config.get("log.print.stderr.show.time.format", baseLogger.dateFormatter)
            showThreadHead    = Config.get("log.print.stderr.show.thread.head", baseLogger.showThreadHead)
            showThreadTail    = Config.get("log.print.stderr.show.thread.tail", baseLogger.showThreadTail)
            ignoreList        = Config.get("log.print.stderr.ignore", baseLogger.ignoreList)
            ignoreLevel       = Config.get("log.print.stderr.ignore.level", baseLogger.ignoreLevel) as Level
            output = new Output(cache ? CACHED_SYS_ERR : SYS_ERR)
        }
    }

    @Override
    List<Level> getLevels() {
        return Level.values().findAll {
            it <= Level.WARN && it <= level
        }.toList()
    }
}
