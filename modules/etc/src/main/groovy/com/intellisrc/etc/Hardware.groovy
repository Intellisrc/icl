package com.intellisrc.etc

import com.intellisrc.core.Cmd
import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.sun.management.OperatingSystemMXBean
import com.sun.management.UnixOperatingSystemMXBean
import groovy.transform.CompileStatic

import java.lang.management.ManagementFactory
import java.util.regex.Matcher

/**
 * Hardware information
 * @since 19/02/26.
 */
@CompileStatic
class Hardware {
    static public boolean debug = Config.getInt("hardware.debug")
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
                Log.d("CPU (pct %.2f)", currentCpuUsage)
            }
        }
        callback(currentCpuUsage)
    }
    
    /**
     * Get Memory Usage
     * @param callback
     */
    static void getMemoryUsage(Metric.MetricChanged callback) {
        String meminfo = new File("/proc/meminfo").text
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
        meminfo.eachLine {
            if(it.startsWith("Buffers")) {
                buffers = getValue(it)
            } else if(it.startsWith("Cached")) {
                cached = getValue(it)
            } else if(it.startsWith("SReclaimable")) {
                sreclaim = getValue(it)
            } else if(it.startsWith("Shmem")) {
                shmem = getValue(it)
            }
            return
        }
        double totalFree = memorySize - (usedMem - (buffers + cached))
        double pct = 0
        if(memorySize) {
            pct = 100 - ((totalFree / memorySize) * 100d)
            if(debug) {
                Log.d("Memory: %.2f GB free / %.2f GB total (%.2f ‰)", bytesToGB(totalFree), bytesToGB(memorySize), pct)
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
                Log.d("Runtime Mem: %.2f GB free / %.2f GB total (%.2f ‰)", bytesToGB(totalFree), bytesToGB(memorySize), pct)
            }
        }
        callback(pct)
    }
    
    /**
     * Get CPU Temperature
     * @param callback
     */
    static void getCpuTemp(Metric.MetricChanged callback) {
        Cmd.async("sensors", {
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
                        Log.d("CPU TEMP: %d", temp)
                    }
                }
                callback(temp)
        }, {
            String out, int code ->
                Log.w("Unable to read CPU temperature")
        })
    }
    
    /**
     * Get GPU Temperature
     * @param callback
     */
    static void getGpuTemp(Metric.MetricChanged callback) {
        Cmd.async("nvidia-smi -q", {
            String out ->
                double temp = 0
                out.readLines().find {
                    if(it.contains("GPU Current Temp")) {
                        Matcher matcher = (it =~ /(\d+)/)
                        if(matcher.find()) {
                            temp = Double.parseDouble(matcher.group(1))
                        }
                    }
                    return temp
                }
                if(temp) {
                    if(debug) {
                        Log.d("GPU TEMP: %d", temp)
                    }
                }
                callback(temp)
        }, {
            String out, int code ->
                Log.w("GPU info is not available")
        })
    }
    
    /**
     * Get GPU memory
     * @param callback
     */
    static void getGpuMem(Metric.MetricChanged callback) {
        Cmd.async("nvidia-smi -q", {
            String out ->
                double total = 0
                double usable = 0
                boolean foundKey = false
                out.readLines().any {
                    if(it.contains("FB Memory Usage")) {
                        foundKey = true
                    }
                    if(foundKey) {
                        if(it.contains("Total")) {
                            Matcher matcher = (it =~ /(\d+)/)
                            if(matcher.find()) {
                                total = matcher.group(1).toDouble()
                            }
                        } else if(it.contains("Free")) {
                            Matcher matcher = (it =~ /(\d+)/)
                            if(matcher.find()) {
                                usable = matcher.group(1).toDouble()
                            }
                        }
                    }
                    return total && usable
                }
                double pct = 0
                if(total) {
                    pct = 100 - ((usable / total).toDouble() * 100d)
                    if(debug) {
                        Log.d("GPU MEM: %.2f GB usable / %.2f GB total (%.2f ‰)", mbToGB(usable), mbToGB(total), pct)
                    }
                }
                callback(pct)
        }, {
            String out, int code ->
                Log.w("GPU info is not available")
        })
    }
    
    /**
     * Get Root HDD Space
     * @param callback
     */
    static void getHddSpace(Metric.MetricChanged callback) {
        final File root = new File("/")
        double usable = root.usableSpace.toDouble()
        double total  = root.totalSpace.toDouble()
        double pct = 0
        if(total) {
            pct = 100 - ((usable / total) * 100d)
            if (pct > 90) {
                Log.w("HDD space is very low. ")
            }
            if(debug) {
                Log.d("HDD: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
            }
        }
        callback(pct)
    }
    /**
     * Get total space in bytes
     * @param root
     * @return
     */
    static double getTotalSpace(File root = new File("/")) {
        return root.totalSpace.toDouble()
    }
    /**
     * Get free space in bytes
     * @param root
     * @return
     */
    static double getFreeSpace(File root = new File("/")) {
        return root.freeSpace.toDouble()
    }
    /**
     * Get Used space in bytes
     * @param root
     * @return
     */
    static double getUsedSpace(File root = new File("/")) {
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
            if (pct > 90) {
                Log.w("Buffer space is very low.")
            }
            if(debug) {
                Log.d("Buffer: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
            }
        }
        callback(pct)
    }
    
    /**
     * Get TMP Space
     * @param callback
     */
    static void getTmpSpace(Metric.MetricChanged callback) {
        final File root = new File("/tmp")
        double usable = root.getUsableSpace().toDouble()
        double total  = root.getTotalSpace().toDouble()
        double pct = 0
        if(total) {
            pct = 100 - ((usable / total) * 100d)
            if (pct > 90) {
                Log.w("Temp space is very low.")
            }
            if(debug) {
                Log.d("TMP: %.2f GB usable / %.2f GB total (%d ‰)", bytesToGB(usable), bytesToGB(total), pct)
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
            Log.d("Unsupported OS")
        }
    }
    
    /**
     * Check if screen is on / off
     * @return
     */
    static boolean getScreenOn() {
        boolean on = false
        if(SysInfo.isWindows()) {
            Log.w("Screen control not available in Windows")
        } else {
            Cmd.async("xset -q", {
                String out ->
                    on = out.contains("Monitor is On")
            }, {
                String out, int code ->
                    Log.w("Unable to detect monitor status")
            })
        }
        return on
    }
    
    /**
     * Turn screen ON / OFF
     * @param on
     */
    static void setScreenOn(boolean on) {
        if(SysInfo.isWindows()) {
            Log.w("Screen control not available in Windows")
        } else {
            Cmd.exec("xset dpms force " + (on ? "on" : "off"), {
                String out, int code ->
                    Log.w("Unable to turn %s screen", on ? "ON" : "OFF")
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
        if(SysInfo.isWindows()) {
            Log.w("Input control not available in Windows")
            return
        }
        String command = enable ? "enable" : "disable"
        new Cmd("xinput list").eachLine({
            String line ->
                if(line.contains("slave")) {
                    if(!device || line.contains(device)) {
                        Matcher matcher = (line =~ /id=(\d+)/)
                        if (matcher) {
                            int id = Integer.parseInt(matcher.group(1))
                            Cmd.exec("xinput --${command} $id")
                        }
                    }
                }
        }).onFail({
            String out, int code ->
                Log.w("Unable to read devices list")
        })
    }
}
