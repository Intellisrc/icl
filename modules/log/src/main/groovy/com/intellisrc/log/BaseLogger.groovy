package com.intellisrc.log

import com.intellisrc.core.Config
import com.intellisrc.core.Version
import groovy.transform.CompileStatic
import org.slf4j.event.Level

/**
 * @since 2021/08/03.
 */
@CompileStatic
class BaseLogger implements Loggable {
    String snapshotLevel      = Config.any.get("log.level.snapshot", "verbose")
    Level level               = getLevelFromString(Version.get().contains("SNAPSHOT") ? snapshotLevel : Config.any.get("log.level", "info"))
    boolean enabled           = Config.any.get("log.enable", ! Config.any.get("log.disable", false))
    boolean useColor          = Config.any.get("log.color", true)
    boolean colorInvert       = Config.any.get("log.color.invert", false)
    boolean showDateTime      = Config.any.get("log.show.time", true)
    boolean showThreadName    = Config.any.get("log.show.thread", true)
    boolean showThreadShort   = Config.any.get("log.show.thread.short", true)
    boolean showLogName       = Config.any.get("log.show.logger", false)
    boolean levelInBrackets   = Config.any.get("log.show.level.brackets", true)
    boolean levelAbbreviated  = Config.any.get("log.show.level.short", true)
    boolean showPackage       = Config.any.get("log.show.package", false)
    boolean showClassName     = Config.any.get("log.show.class", true)
    boolean showMethod        = Config.any.get("log.show.method", true)
    boolean showLineNumber    = Config.any.get("log.show.line.number", true)
    boolean showStackTrace    = Config.any.get("log.show.stack", true)
    String dateFormatter      = Config.any.get("log.show.time.format","yyyy-MM-dd HH:mm:ss.SSS")
    int showThreadHead        = Config.any.get("log.show.thread.head", 2)
    int showThreadTail        = Config.any.get("log.show.thread.tail", 2)
    // It will ignore any INFO or below in the following classes/packages:
    List<String> ignoreList   = Config.any.getList("log.ignore")
    Level ignoreLevel         = Config.any.get("log.ignore.level", Level.DEBUG) as Level

    /**
     * Convert string to Level
     * @param string
     * @return
     */
    static Level getLevelFromString(String string) {
        if(string.toUpperCase() == "VERBOSE") {
            string = "trace"
        }
        return string.toUpperCase() as Level
    }
}
