package com.intellisrc.log

import groovy.transform.CompileStatic

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * This Logger prints into a ByteArray, which can be converted into string
 * Useful to implement custom loggers or for testing
 * @since 2021/08/11.
 */
@CompileStatic
class StringLogger extends BaseLogger implements LoggableOutputLevels {
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    Charset utf8 = StandardCharsets.UTF_8
    PrintStream ps = new PrintStream(baos, true, utf8.name())
    Output.OnPrint onPrint = null

    StringLogger(Output.OnPrint onPrint = null) {
        this.onPrint = onPrint
    }

    @Override
    Output getOutput() {
        return new Output(ps, onPrint)
    }

    String getContent() {
        return new String(baos.toByteArray(), utf8)
    }

    String getBytes() {
        return baos.toByteArray()
    }

    boolean clear() {
        baos.reset()
        return true
    }

    void close() {
        ps.close()
        baos.close()
    }
}
