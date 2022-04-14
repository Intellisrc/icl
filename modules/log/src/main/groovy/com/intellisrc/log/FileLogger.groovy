package com.intellisrc.log

import com.intellisrc.core.Config
import com.intellisrc.core.Millis
import com.intellisrc.core.SysClock
import groovy.transform.CompileStatic
import org.slf4j.event.Level

import java.lang.reflect.Method
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.regex.Matcher

/**
 * @since 2021/08/02.
 */
@CompileStatic
class FileLogger extends BaseLogger implements LoggableOutputLevels {
    //---------------------- STATIC ----------------------
    protected static synchronized final List<OnCleanDone> onCleanList = []
    static void setOnCleanDone(OnCleanDone toSet) {
        onCleanList << toSet
    }
    /**
     * Called after we are done cleaning
     */
    static interface OnCleanDone {
        void call()
    }

    //---------------------- INSTANCE --------------------
    protected boolean initialized = false
    protected Output output
    protected int logDays               = 7
    protected File logDir               = File.get("log")
    protected String logFileName        = "system.log"
    protected boolean rotateOtherLogs   = false  //When true it will also remove other old logs in the log directory
    protected boolean compress          = false
    protected int maxTaskExecTimeMs     = Millis.MINUTE
    protected LocalDate today           = SysClock.now.toLocalDate()  // Keep track of day, so we can change log name when date changes

    /**
     * Package access allows only {@link CommonLoggerFactory} to instantiate
     * CommonLogger instances.
     */
    @Override
    void initialize(Loggable baseLogger) {
        if(initialized) { return }
        initialized = true
        enabled = Config.get("log.file", true)
        // File output:
        if(enabled) {
            level             = getLevelFromString(Config.get("log.file.level", baseLogger.level.toString()))
            useColor          = Config.get("log.file.color", baseLogger.useColor)
            colorInvert       = Config.get("log.file.color.invert", baseLogger.colorInvert)
            showDateTime      = Config.get("log.file.show.time", baseLogger.showDateTime)
            showThreadName    = Config.get("log.file.show.thread", baseLogger.showThreadName)
            showThreadShort   = Config.get("log.file.show.thread.short", baseLogger.showThreadShort)
            showLogName       = Config.get("log.file.show.logger", baseLogger.showLogName)
            levelInBrackets   = Config.get("log.file.show.level.brackets", baseLogger.levelInBrackets)
            levelAbbreviated  = Config.get("log.file.show.level.short", baseLogger.levelAbbreviated)
            showPackage       = Config.get("log.file.show.package", baseLogger.showPackage)
            showClassName     = Config.get("log.file.show.class", baseLogger.showClassName)
            showMethod        = Config.get("log.file.show.method", baseLogger.showMethod)
            showLineNumber    = Config.get("log.file.show.line.number", baseLogger.showLineNumber)
            dateFormatter     = Config.get("log.file.show.time.format", baseLogger.dateFormatter)
            showThreadHead    = Config.get("log.file.show.thread.head", baseLogger.showThreadHead)
            showThreadTail    = Config.get("log.file.show.thread.tail", baseLogger.showThreadTail)
            showStackTrace    = Config.get("log.file.show.stack", baseLogger.showStackTrace)
            ignoreList        = Config.get("log.file.ignore", baseLogger.ignoreList)
            ignoreLevel       = Config.get("log.file.ignore.level", baseLogger.ignoreLevel) as Level

            // Specific settings for File
            logFileName       = Config.get("log.file.name", logFileName)
            logDir            = Config.get("log.file.dir", logDir)
            compress          = Config.get("log.file.compress", true)
            logDays           = Config.get("log.file.days", logDays)
            rotateOtherLogs   = Config.get("log.file.rotate.other", rotateOtherLogs)
            maxTaskExecTimeMs = Config.get("log.file.max.exec", maxTaskExecTimeMs)

            if(! logDir.exists())   { logDir.mkdirs() }
            output = new Output(logFile)

            // Clean and link logs on init
            cleanLogs()
            linkLast()
        }
    }

    File getLogFile() {
        return new File(logDir, logFileName)
    }
    /**
     * Change Log file name
     * @param newName
     */
    void setLogFileName(String newName) {
        logFileName = newName
        if(initialized) {
            output = new Output(logFile)
            linkLast(true)
        }
    }
    /**
     * Change Log dir
     * @param newDir
     */
    void setLogDir(File newDir) {
        logDir = newDir
        if(initialized) {
            if(!logDir.exists()) {
                logDir.mkdirs()
            }
            output = new Output(logFile)
            linkLast(true)
        }
    }

    @Override
    Output getOutput() {
        LocalDate current = SysClock.now.toLocalDate()
        if(current != today) {
            if(current > today) {
                logFile.renameTo(File.get(logDir, today.YMD + "-" + logFileName))
            }
            today = current
            this.output = new Output(logFile)
            linkLast(true)
        }
        return this.output
    }

    @Override
    List<Level> getLevels() {
        return Level.values().findAll {
            it <= level
        }.toList()
    }
    /**
     * Remove all logs in log directory
     * @return
     */
    boolean removeLogs() {
        logDir.eachFile {
            it.delete()
        }
        return logDir.listFiles().toList().empty
    }
    /**
     * Compress Log file if Zip is present
     */
    boolean compressLog(File logToCompress = logFile) {
        boolean done = false
        if(compress && logToCompress?.exists()) {
            try {
                Class[] parameters = [File.class]
                String thisPkg = this.class.package.name.tokenize(".").last()
                Class zip = Class.forName(this.class.package.name.replace(thisPkg, 'etc') + ".Zip")
                Method method = zip.getMethod("gzip", parameters)
                Object[] callParams = [logToCompress]
                method.invoke(null, callParams)
                File gziped = new File(logToCompress.parentFile, logToCompress.name + ".gz")
                gziped.setLastModified(getLogDateTime(logFile).toMillis()) //Set the date and time in which was last modified
                done = gziped.exists()
            } catch (Exception ignored) {
                //Ignore... Zip class doesn't exists, so we don't compress them
                //We can't use print here or it will loop forever
            }
        }
        return done
    }

    /**
     * Return log date
     * @param file
     * @return
     */
    static LocalDateTime getLogDateTime(final File file) {
        long modified = file.lastModified()
        LocalDateTime date
        if(modified > 0) {
            date = LocalDateTime.fromMillis(modified)
        } else {
            Matcher m = (file.name =~ /(\d{2,4}-?\d{2}-?\d{2,4})/)
            if(m.find()) {
                date = m.group(1).toDate().atStartOfDay()
            } else {
                date = SysClock.now
            }
        }
        return date
    }

    /**
     * Delete old logs and compress as needed
     * @param logsDir : Logs directory
     * @param logName : Name of log, e.g : system.log
     * @param days : number of logs to keep (usually 1 per day)
     */
    void cleanLogs() {
        if(logDir?.exists()) {
            Runnable runnable = {
                // Compress old not compressed logs in directory:
                String pattern = rotateOtherLogs ? "*.log" : "*-" + logFileName
                logDir.eachFileMatchAsync(pattern) {
                    File log ->
                        if (!Files.isSymbolicLink(log.toPath())) { //Skip links
                            LocalDateTime lastMod = getLogDateTime(log).toLocalDate().atStartOfDay()
                            if (ChronoUnit.DAYS.between(lastMod, SysClock.dateTime) >= logDays) {
                                log.delete() // This removes old logs when ZIP is not available
                            } else if (ChronoUnit.DAYS.between(lastMod, SysClock.dateTime) > 0) {
                                compressLog(log)
                            }
                        }
                }
                // Compress old not compressed logs in directory:
                String patternZip = rotateOtherLogs ? "*.gz" : "*-" + logFileName + "*.gz"
                logDir.eachFileMatchAsync(patternZip) {
                    File log ->
                        if (!Files.isSymbolicLink(log.toPath())) { //Skip links
                            LocalDateTime lastMod = getLogDateTime(log).toLocalDate().atStartOfDay()
                            if (ChronoUnit.DAYS.between(lastMod, SysClock.dateTime) >= logDays) {
                                log.delete() // This removes old logs when they are compressed
                            }
                        }
                }
                if(!onCleanList.isEmpty()) {
                    onCleanList.each {
                        OnCleanDone onDone ->
                            onDone.call()
                    }
                }
            }
            try {
                Class tasks = Class.forName(this.class.package.name.replace('core', 'thread') + ".Tasks")
                Class priority = Class.forName(this.class.package.name.replace('core', 'thread') + '.Task$Priority')
                Class[] parameters = [Runnable.class, String.class, priority, Integer.TYPE]
                Method method = tasks.getMethod("add", parameters)
                Object low = Enum.valueOf(priority as Class, "LOW")
                Object[] callParams = [runnable, "Log.clean", low, maxTaskExecTimeMs]
                method.invoke(null, callParams)
            } catch (Exception ignored) {
                //Ignore... Tasks class doesn't exists, so we use a normal Thread:
                //We can't use print here or it will loop forever
                new Thread(runnable).start()
            }
        }
    }

    /**
     * Link last log
     */
    void linkLast(boolean update = true) {
        if(logFile?.exists()) {
            File link = new File(logDir, "last-" + logFileName)
            if (link.exists()) {
                if(update) {
                    link.delete()
                    logFile.linkTo(link)
                }
            } else {
                try {
                    link.toPath().toRealPath().toFile().absolutePath
                } catch(Exception ignored) {
                    link.delete() // link.exists will fail if link is broken. In that case is better to be sure to remove it
                }
                logFile.linkTo(link)
            }
        }
    }
}
