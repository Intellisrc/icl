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
    String snapshotLevel      = Config.get("log.level.snapshot", "verbose")
    Level level               = getLevelFromString(Version.get().contains("SNAPSHOT") ? snapshotLevel : Config.get("log.level", "info"))
    boolean enabled           = Config.get("log.enable", ! Config.get("log.disable", false))
    boolean useColor          = Config.get("log.color", true)
    boolean colorInvert       = Config.get("log.color.invert", false)
    boolean showDateTime      = Config.get("log.show.time", true)
    boolean showThreadName    = Config.get("log.show.thread", true)
    boolean showThreadShort   = Config.get("log.show.thread.short", true)
    boolean showLogName       = Config.get("log.show.logger", false)
    boolean levelInBrackets   = Config.get("log.show.level.brackets", true)
    boolean levelAbbreviated  = Config.get("log.show.level.short", true)
    boolean showPackage       = Config.get("log.show.package", false)
    boolean showClassName     = Config.get("log.show.class", true)
    boolean showMethod        = Config.get("log.show.method", true)
    boolean showLineNumber    = Config.get("log.show.line.number", true)
    boolean showStackTrace    = Config.get("log.show.stack", true)
    String dateFormatter      = Config.get("log.show.time.format","yyyy-MM-dd HH:mm:ss.SSS")
    int showThreadHead        = Config.get("log.show.thread.head", 2)
    int showThreadTail        = Config.get("log.show.thread.tail", 2)
    // It will ignore any INFO or below in the following classes/packages:
    List<String> ignoreList   = Config.getList("log.ignore")
    Level ignoreLevel         = Config.get("log.ignore.level", Level.DEBUG) as Level

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
