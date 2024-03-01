package com.intellisrc.etc

import com.intellisrc.core.Cmd
import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.sun.management.OperatingSystemMXBean
import com.sun.management.UnixOperatingSystemMXBean
import groovy.transform.CompileStatic

import java.lang.management.ManagementFactory
import java.util.regex.Matcher

import static com.intellisrc.core.SysInfo.isWindows

/**
 * Hardware information
 * @since 19/02/26.
 */
@CompileStatic
class Hardware {
    static public boolean debug     = Config.any.get("hardware.debug", false)
    static public boolean warn      = Config.any.get("hardware.warn", true)
    static public boolean gpu       = Config.any.get("hardware.gpu", true)
    static public boolean monitor   = Config.any.get("hardware.monitor", true)

    /**
     * Default commands
     */
    static private final String sensorsCmd  = "sensors"
    static private final String nvidiaSMI   = "nvidia-smi -q"
    static private final String screenCheck = "xset -q"
    static private final String screenON    = "xset dpms force on"
    static private final String screenOFF   = "xset dpms force off"
    static private final String xinput      = "xinput"
    static private final String xinputList  = "xinput list"

    /**
     * Custom commands (override)
     */
    static public String customSensorsCmd  = Config.any.get("hardware.cmd.sensors")
    static public String customNvidiaSMI   = Config.any.get("hardware.cmd.nvidia.smi")
    static public String customScreenCheck = Config.any.get("hardware.cmd.screen")
    static public String customScreenON    = Config.any.get("hardware.cmd.screen.on")
    static public String customScreenOFF   = Config.any.get("hardware.cmd.screen.off")
    static public String customXinput      = Config.any.get("hardware.cmd.xinput")
    static public String customXinputList  = Config.any.get("hardware.cmd.xinput.list")

    static private OperatingSystemMXBean os
    
    /**
     * Get Operating System object. Set it only once.
     * @return
     */
    static OperatingSystemMXBean getOperatingSystem() {
        if(!os) {
            os = ((OperatingSystemMXBean) ManagementFactory.operatingSystemMXBean)
        }
        return os
    }

    static bytesToGB(double bytes) {
        return bytes / (1024*1024*1024)
    }
    static mbToGB(double bytes) {
        return bytes / 1024
    }
    
    /**
     * Get CPU Usage
     * @param callback
     */
    static void getCpuUsage(Metric.MetricChanged callback) {
        double value = operatingSystem.systemCpuLoad
        // usually takes a couple of seconds before we get real values
        double currentCpuUsage = 0
        if (value > 0) {
            // returns a percentage value with 1 decimal point precision
            currentCpuUsage = (value * 1000) / 10d
            if(debug) {
                Log.v("CPU (pct %.2f)", currentCpuUsage)
            }
        }
        callback(currentCpuUsage)
    }
    
    /**
     * Get Memory Usage
     * @param callback
     */
    static void getMemoryUsage(Metric.MetricChanged callback) {
        double memorySize = operatingSystem.totalPhysicalMemorySize.toDouble()
        double usedMem = memorySize - operatingSystem.freePhysicalMemorySize.toDouble()
        double buffers = 0
        double cached = 0
        double sreclaim = 0
        double shmem = 0
        def getValue = {
            String line ->
                return line.tokenize(" ")[1].toDouble() * 1024
        }
        if(! isWindows()) {
            String meminfo = File.get("/proc/meminfo").text
            meminfo.eachLine {
                if (it.startsWith("Buffers")) {
                    buffers = getValue(it)
                } else if (it.startsWith("Cached")) {
                    cached = getValue(it)
                } else if (it.startsWith("SReclaimable")) {
                    sreclaim = getValue(it)
                } else if (it.startsWith("Shmem")) {
                    shmem = getValue(it)
                }
                return
            }
        }
        double totalFree = memorySize - (usedMem - (buffers + cached))
        double pct = 0
        if(memorySize) {
            pct = 100 - ((totalFree / memorySize) * 100d)
            if(debug) {
                Log.v("Memory: %.2f GB free / %.2f GB total (%.2f ‰)", bytesToGB(totalFree), bytesToGB(memorySize), pct)
            }
        }
        callback(pct)
    }
    
    /**
     * Calculate total memory used by Java
     * @param callback
     */
    static void getRuntimeMemoryUsage(Metric.MetricChanged callback) {
        double memorySize = Runtime.runtime.maxMemory()
        double usedMem = Runtime.runtime.totalMemory() - Runtime.runtime.freeMemory()
        double totalFree = memorySize - usedMem
        double pct = 0
        if(memorySize) {
            pct = 100 - ((totalFree / memorySize) * 100d)
            if(debug) {
                Log.v("Runtime Mem: %.2f GB free / %.2f GB total (%.2f ‰)", bytesToGB(totalFree), bytesToGB(memorySize), pct)
            }
        }
        callback(pct)
    }
    
    /**
     * Get CPU Temperature
     * @param callback
     */
    static void getCpuTemp(Metric.MetricChanged callback) {
        Cmd.async(customSensorsCmd ?: sensorsCmd, {
            String out ->
                double temp = 0
                List<Double> temps = []
                out.eachLine {
                    Matcher matcher = (it =~ /:\s+\+(\d+.\d)/)
                    if(matcher.find()) {
                        temps << Double.parseDouble(matcher.group(1))
                    }
                    return
                }
                if(!temps.empty) {
                    temp = temps.max()
                    if(debug) {
                        Log.v("CPU TEMP: %d", temp)
                    }
                }
                callback(temp)
        }, {
            String out, int code ->
                if(warn) {
                    Log.w("Unable to read CPU temperature")
                }
        })
    }
    
    /**
     * Get GPU Temperature
     * @param callback
     */
    static void getGpuTemp(Metric.MetricChanged callback) {
        if(gpu) {
            Cmd.async(customNvidiaSMI ?: nvidiaSMI, {
                String out ->
                    double temp = 0
                    out.readLines().find {
                        if (it.contains("GPU Current Temp")) {
                            Matcher matcher = (it =~ /(\d+)/)
                            if (matcher.find()) {
                                temp = Double.parseDouble(matcher.group(1))
                            }
                        }
                        return temp
                    }
                    if (temp) {
                        if (debug) {
                            Log.v("GPU TEMP: %d", temp)
                        }
                    }
                    callback(temp)
            }, {
                String out, int code ->
                    if(warn) {
                        Log.w("GPU info is not available")
                        Log.v("You can disable last message by setting 'hardware.gpu=false' in config.properties")
                    }
            })
        }
    }
    
    /**
     * Get GPU memory
     * @param callback
     */
    static void getGpuMem(Metric.MetricChanged callback) {
        if(gpu) {
            Cmd.async(customNvidiaSMI ?: nvidiaSMI, {
                String out ->
                    double total = 0
                    double usable = 0
                    boolean foundKey = false
                    out.readLines().any {
                        if (it.contains("FB Memory Usage")) {
                            foundKey = true
                        }
                        if (foundKey) {
                            if (it.contains("Total")) {
                                Matcher matcher = (it =~ /(\d+)/)
                                if (matcher.find()) {
                                    total = matcher.group(1).toDouble()
                                }
                            } else if (it.contains("Free")) {
                                Matcher matcher = (it =~ /(\d+)/)
                                if (matcher.find()) {
                                    usable = matcher.group(1).toDouble()
                                }
                            }
                        }
                        return total && usable
                    }
                    double pct = 0
                    if (total) {
                        pct = 100 - ((usable / total).toDouble() * 100d)
                        if (debug) {
                            Log.v("GPU MEM: %.2f GB usable / %.2f GB total (%.2f ‰)", mbToGB(usable), mbToGB(total), pct)
                        }
                    }
                    callback(pct)
            }, {
                String out, int code ->
                    if(warn) {
                        Log.w("GPU info is not available")
                        Log.v("You can disable last message by setting 'hardware.gpu=false' in config.properties")
                    }
            })
        }
    }
    
    /**
     * Get Root HDD Space
     * @param callback
     */
    static void getHddSpace(Metric.MetricChanged callback) {
        final File root = File.rootDir
        double usable = root.usableSpace.toDouble()
        double total  = root.totalSpace.toDouble()
        double pct = 0
        if(total) {
            pct = 100 - ((usable / total) * 100d)
            if (pct > 90 && warn) {
                Log.w("HDD free space is low. ")
            }
            if(debug) {
                Log.v("HDD: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
            }
        }
        callback(pct)
    }
    /**
     * Get total space in bytes
     * @param root
     * @return
     */
    static double getTotalSpace(File root = File.rootDir) {
        return root.totalSpace.toDouble()
    }
    /**
     * Get free space in bytes
     * @param root
     * @return
     */
    static double getFreeSpace(File root = File.rootDir) {
        return root.freeSpace.toDouble()
    }
    /**
     * Get Used space in bytes
     * @param root
     * @return
     */
    static double getUsedSpace(File root = File.rootDir) {
        return root.usableSpace.toDouble()
    }
    
    /**
     * Get specific drive space
     * @param root
     * @param callback
     */
    static void getDriveSpace(File root, Metric.MetricChanged callback) {
        double usable = root.usableSpace.toDouble()
        double total  = root.totalSpace.toDouble()
        double pct = 0
        if(total) {
            pct = 100 - ((usable / total) * 100d)
            if (pct > 90 && warn) {
                Log.w("Buffer space is very low.")
            }
            if(debug) {
                Log.v("Buffer: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
            }
        }
        callback(pct)
    }
    
    /**
     * Get TMP Space
     * @param callback
     */
    static void getTmpSpace(Metric.MetricChanged callback) {
        final File root = File.tempDir
        double usable = root.getUsableSpace().toDouble()
        double total  = root.getTotalSpace().toDouble()
        double pct = 0
        if(total) {
            pct = 100 - ((usable / total) * 100d)
            if (pct > 90 && warn) {
                Log.w("Temp space is very low.")
            }
            if(debug) {
                Log.v("TMP: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
            }
        }
        callback(pct)
    }

    /**
     * Get the pct of open files in the OS (only Linux is supported for now)
     * @param callback
     * @param maxOpen : if set, it will use that to calculate pct instead the one from OS
     * @param absolute: if true, will return the absolute number of file opened and not a percentage
     *
     * NOTE: You can also check it with: lsof -c java | wc -l
     */
    static void getOpenFiles(Metric.MetricChanged callback) {
        getOpenFiles(callback, 0, true)
    }
    static void getOpenFilesPct(Metric.MetricChanged callback, long maxOpen = 0) {
        getOpenFiles(callback, maxOpen, false)
    }
    protected static void getOpenFiles(Metric.MetricChanged callback, long maxOpen, boolean absolute) {
        if(operatingSystem instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unix = operatingSystem as UnixOperatingSystemMXBean
            long max = maxOpen ?: unix.getMaxFileDescriptorCount()
            long current = unix.getOpenFileDescriptorCount()
            callback(absolute ? current : (current / max) as double)
        } else {
            Log.v("Unsupported OS")
        }
    }
    
    /**
     * Check if screen is on / off
     * @return
     */
    static boolean getScreenOn() {
        boolean on = false
        if(monitor) {
            Cmd.async(customScreenCheck ?: screenCheck, {
                String out ->
                    on = out.contains("Monitor is On")
            }, {
                String out, int code ->
                    if(warn) {
                        if (!customScreenCheck && isWindows()) {
                            Log.w("Windows is not supported by default. You can implement your own command and" +
                                "override Hardware.customScreenCheck or set 'hardware.cmd.screen' in config.properties")
                        } else {
                            Log.w("Unable to detect monitor status")
                        }
                    }
            })
        }
        return on
    }
    
    /**
     * Turn screen ON / OFF
     * @param on
     */
    static void setScreenOn(boolean on) {
        if(monitor) {
            Cmd.exec(on ? (customScreenON ?: screenON) : (customScreenOFF ?: screenOFF), {
                String out, int code ->
                    String status = on ? "ON" : "OFF"
                    if(warn) {
                        if (!customScreenON && isWindows()) {
                            Log.w("Windows is not supported by default. You can implement your own command and" +
                                "override Hardware.customScreen${status} or set 'hardware.cmd.screen.${status.toLowerCase()}' " +
                                "in config.properties")
                        } else {
                            Log.w("Unable to turn %s screen", status)
                        }
                    }
            })
        }
    }
    
    /**
     * Enable input device
     * For example:
     * enableInputDevice("USB Mouse", false)
     * enableInputDevice("Keyboard", false)
     * enableInputDevice() //ALL
     *
     * @param device
     */
    static void enableInputDevice(String device = "") {
        disableEnableInputDeviceCommon(device, true)
    }
    /**
     * Disable input device
     * For example:
     * disableInputDevice("USB Mouse", false)
     * disableInputDevice("Keyboard", false)
     * disableInputDevice()
     *
     * @param device
     */
    static void disableInputDevice(String device = "") {
        disableEnableInputDeviceCommon(device, false)
    }
    
    /** Shortcuts **/
    static void enablePointer() {
        enableInputDevice("pointer")
    }
    static void enableKeyboard() {
        enableInputDevice("keyboard")
    }
    static void disablePointer() {
        disableInputDevice("pointer")
    }
    static void disableKeyboard() {
        disableInputDevice("keyboard")
    }
    
    /**
     * Common code between
     * @param device
     * @param enable
     */
    static private void disableEnableInputDeviceCommon(String device = "", boolean enable) {
        String command = enable ? "enable" : "disable"
        new Cmd(customXinputList ?: xinputList).eachLine({
            String line ->
                if(line.contains("slave")) {
                    if(!device || line.contains(device)) {
                        Matcher matcher = (line =~ /id=(\d+)/)
                        if (matcher) {
                            int id = Integer.parseInt(matcher.group(1))
                            Cmd.exec([customXinput ?: xinput, "--${command}", id])
                        }
                    }
                }
        }).onFail({
            String out, int code ->
                if(warn) {
                    if (!customXinputList && isWindows()) {
                        Log.w("Windows is not supported by default. You can implement your own command and" +
                            "override Hardware.customXinput and Hardware.customXinputList or " +
                            "set 'hardware.cmd.xinput' and 'hardware.cmd.xinput.list' in config.properties")
                    } else {
                        Log.w("Unable to read devices list")
                    }
                }
        })
    }
}
