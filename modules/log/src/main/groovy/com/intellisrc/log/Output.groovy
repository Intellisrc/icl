package com.intellisrc.log

import groovy.transform.CompileStatic
import org.slf4j.event.Level

import static com.intellisrc.log.Output.OutputType.*

/**
 * Based on: org.slf4j.simple.OutputChoice
 * @since 2021/08/02.
 */
@CompileStatic
class Output {
    enum OutputType {
        SYS_OUT, CACHED_SYS_OUT, SYS_ERR, CACHED_SYS_ERR, FILE
        PrintStream getPrintStream() {
            switch (this) {
                case SYS_OUT:
                case CACHED_SYS_OUT:
                    return System.out
                case SYS_ERR:
                case CACHED_SYS_ERR:
                    return System.err
                default: //FILE (it should never enter here)
                    return null
            }
        }
    }
    protected OutputType outputChoiceTypeValue = null
    protected PrintStream targetPrintStreamValue = null

    Output(OutputType outputChoiceType) {
        outputChoiceTypeValue = outputChoiceType
        targetPrintStreamValue = outputChoiceType.printStream
        if (outputChoiceType == FILE) {
            throw new IllegalArgumentException()
        }
    }

    Output(PrintStream printStream) {
        outputChoiceTypeValue = FILE
        targetPrintStreamValue = printStream
    }

    Output(File file) {
        this(new PrintStream(file.absolutePath))
    }

    PrintStream getTargetPrintStream() {
        return targetPrintStreamValue
    }
}
