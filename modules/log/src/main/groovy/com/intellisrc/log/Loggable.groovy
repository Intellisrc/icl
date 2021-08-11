package com.intellisrc.log

import groovy.transform.CompileStatic
import org.slf4j.event.Level

/**
 * @since 2021/08/02.
 */
@CompileStatic
trait Loggable {
    abstract Level getLevel()
    abstract boolean isEnabled()
    abstract boolean getUseColor()
    abstract boolean getColorInvert()
    abstract boolean getShowDateTime()
    abstract boolean getShowThreadName()
    abstract boolean getShowThreadShort()
    abstract boolean getShowLogName()
    abstract boolean getLevelInBrackets()
    abstract boolean getLevelAbbreviated()
    abstract boolean getShowPackage()
    abstract boolean getShowClassName()
    abstract boolean getShowMethod()
    abstract boolean getShowLineNumber()
    abstract String getDateFormatter()
    abstract int getShowThreadHead()
    abstract int getShowThreadTail()
    abstract List<String> getIgnoreList()
    abstract Level getIgnoreLevel()
}