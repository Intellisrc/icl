package com.intellisrc.log

import groovy.transform.CompileStatic
import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider
/**
 * @since 2021/08/02.
 */
@CompileStatic
class CommonLoggerService implements SLF4JServiceProvider {
    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is modified with each major release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "2.0.9" // !final

    private ILoggerFactory loggerFactory
    private IMarkerFactory markerFactory
    private MDCAdapter mdcAdapter

    ILoggerFactory getLoggerFactory() {
        return loggerFactory
    }

    @Override
    IMarkerFactory getMarkerFactory() {
        return markerFactory
    }

    @Override
    MDCAdapter getMDCAdapter() {
        return mdcAdapter
    }

    @Override
    String getRequestedApiVersion() {
        return REQUESTED_API_VERSION
    }

    @Override
    void initialize() {
        loggerFactory = new CommonLoggerFactory()
        markerFactory = new BasicMarkerFactory()
        mdcAdapter = new NOPMDCAdapter()
    }
}
