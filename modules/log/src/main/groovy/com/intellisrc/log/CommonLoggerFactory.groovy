package com.intellisrc.log

import groovy.transform.CompileStatic
import org.slf4j.ILoggerFactory
import org.slf4j.Logger

/**
 * @since 2021/08/02.
 */
@CompileStatic
class CommonLoggerFactory implements ILoggerFactory {
    protected Logger logger

    /**
     * Return an appropriate {@link CommonLogger} instance by name.
     */
    @Override
    Logger getLogger(String name) {
        if (! logger) {
            logger = new CommonLogger()
        }
        return logger
    }

    /**
     * Add a printer:
     * <code>
     *     (LoggerFactory.getLogger("default") as CommonLogger).addPrinter(...)
     * </code>
     * @param printer
     */
    void addPrinter(LoggableOutputLevels printer) {
        (logger as CommonLogger).addPrinter(printer)
    }
}
