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
    interface OnPrint {
        void call(String msg)
    }
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
    protected OnPrint onPrint = null

    Output(OutputType outputChoiceType, OnPrint onPrint = null) {
        outputChoiceTypeValue = outputChoiceType
        targetPrintStreamValue = outputChoiceType.printStream
        this.onPrint = onPrint
        if (outputChoiceType == FILE) {
            throw new IllegalArgumentException()
        }
    }

    Output(PrintStream printStream, OnPrint onPrint = null) {
        outputChoiceTypeValue = FILE
        targetPrintStreamValue = printStream
        this.onPrint = onPrint
    }

    Output(File file, OnPrint onPrint = null) {
        this(new PrintStream(file.absolutePath), onPrint)
    }

    PrintStream getTargetPrintStream() {
        return targetPrintStreamValue
    }
}
