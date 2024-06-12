package com.intellisrc.log

import com.intellisrc.core.Config
import groovy.transform.CompileStatic
import org.slf4j.event.Level

import static com.intellisrc.log.Output.OutputType.CACHED_SYS_ERR
import static com.intellisrc.log.Output.OutputType.SYS_ERR

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
            level             = getLevelFromString(Config.any.get("log.print.stderr.level", baseLogger.level.toString()))
            useColor          = Config.any.get("log.print.stderr.color", baseLogger.useColor)
            colorInvert       = Config.any.get("log.print.stderr.color.invert", baseLogger.colorInvert)
            showDateTime      = Config.any.get("log.print.stderr.show.time", baseLogger.showDateTime)
            showThreadName    = Config.any.get("log.print.stderr.show.thread", baseLogger.showThreadName)
            showThreadShort   = Config.any.get("log.print.stderr.show.thread.short", baseLogger.showThreadShort)
            showLogName       = Config.any.get("log.print.stderr.show.logger", baseLogger.showLogName)
            levelInBrackets   = Config.any.get("log.print.stderr.show.level.brackets", baseLogger.levelInBrackets)
            levelAbbreviated  = Config.any.get("log.print.stderr.show.level.short", baseLogger.levelAbbreviated)
            showPackage       = Config.any.get("log.print.stderr.show.package", baseLogger.showPackage)
            showClassName     = Config.any.get("log.print.stderr.show.class", baseLogger.showClassName)
            showMethod        = Config.any.get("log.print.stderr.show.method", baseLogger.showMethod)
            showLineNumber    = Config.any.get("log.print.stderr.show.line.number", baseLogger.showLineNumber)
            dateFormatter     = Config.any.get("log.print.stderr.show.time.format", baseLogger.dateFormatter)
            showThreadHead    = Config.any.get("log.print.stderr.show.thread.head", baseLogger.showThreadHead)
            showThreadTail    = Config.any.get("log.print.stderr.show.thread.tail", baseLogger.showThreadTail)
            showStackTrace    = Config.any.get("log.print.stderr.show.stack", baseLogger.showStackTrace)
            ignoreList        = Config.any.get("log.print.stderr.ignore", baseLogger.ignoreList)
            ignoreLevel       = Config.any.get("log.print.stderr.ignore.level", baseLogger.ignoreLevel) as Level
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
