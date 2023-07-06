package com.intellisrc.log

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.slf4j.event.Level

import java.time.LocalDateTime

import static com.intellisrc.log.Output.OutputType.FILE

/**
 * Based on: org.slf4j.simple.OutputChoice
 * @since 2021/08/02.
 */
@CompileStatic
class Output {
    static class LogDetail {
        LocalDateTime time
        Level level
        String message
        Log.Info location
        Throwable exception
        String formatted
    }
    interface OnPrint {
        void call(LogDetail info)
    }
    enum OutputType {
        SYS_OUT, CACHED_SYS_OUT, SYS_ERR, CACHED_SYS_ERR, FILE
        PrintStream getPrintStream() {
            //noinspection GroovyFallthrough
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
        this(new PrintStream(new FileOutputStream(file.absolutePath, true)), onPrint)
    }

    PrintStream getTargetPrintStream() {
        return targetPrintStreamValue
    }
}
