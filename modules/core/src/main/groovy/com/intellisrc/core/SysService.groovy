package com.intellisrc.core

import groovy.transform.CompileStatic

import java.lang.reflect.Method

/**
 * @since 17/10/23.
 *
 * In order to use this class, specify:
 *
 * <code>
 * class MyClassService extends SysService {
 *     static {
 *         service = new MyClassService()
 *         //To change default lock file (optional):
 *         service.lockFile = "my_lock_file.zzz"
 *     }
 *     ...
 * }
 * </code>
 *
 * or in config.properties:
 * main.class = my.domain.pkg.Main
 *
 * or as System Property (environment) with key 'main.class'
 *
 * NOTE:
 * ---------------------------------------------------------------------------------------------
 * Setting lockFile may be required when two services are running in the same directory or
 * two or more services are running from read-only directories (as it will use TEMP dir instead)
 *
 * Custom Actions:
 * ------------------------
 * You can add custom methods for actions like:
 * onReload() , which will be called when first argument is: "reload"
 *
 * Initialization
 * ------------------------
 * You can override onInit() which will be called before assigning action
 * It is useful if you want do something with the arguments (stored in args)
 * before starting.
 */
@CompileStatic
abstract class SysService {
    static public SysService service
    static public int exitCode = 0
    static private boolean started = false
    /**
     * Initialize service with arguments
     * @param args
     */
    static void main(String[] args) {
        if(service == null) {
            String cfgMain = Config.get("main.class", Config.system.get("main.class"))
            if(cfgMain) {
                try {
                    Class c = Class.forName(cfgMain)
                    service = c.getConstructor().newInstance() as SysService
                } catch(Exception e) {
                    Log.e("Unable to initialize main class", e)
                }
            } else {
                Log.e("`service` is not defined. To define it, set it in 'config.properties' " +
                    "e.g. 'main.class = org.example.app.MyClass' or use: " + SysInfo.newLine +
                    " static { service = new MyClass() }" + SysInfo.newLine +
                    " inside MyClass")
                System.exit(3)
            }
        }
        Version.mainClass = service.class
        SysService sysSrv = service
        sysSrv.args.clear()
        sysSrv.args.addAll(args.toList())
        String action = sysSrv.args.isEmpty() ? "start" : sysSrv.args.first()
        if(action != "stop") {
            //Initialize
            sysSrv.onInit()
        }
        File usrDir = File.userDir
        if(! usrDir.canWrite()) {
            usrDir = File.tempDir
        }
        File lockFile = new File(usrDir, sysSrv.lockFile)
        //noinspection GroovyFallthrough
        switch(action) {
            case "stop" :
                if(started) {
                    Log.v("Removing lock file")
                    lockFile.delete()
                    started = false
                }
                service.onStop()
                service.kill(exitCode)
                // It will exit in the previous line
                break
            case "restart":
                if(sysSrv.args.size() > 0) {
                    sysSrv.args.poll()
                }
                sysSrv.onRestart()
                //no break
            case "start":
                if(sysSrv.args.size() > 0 && sysSrv.args.first() == "start") {
                    sysSrv.args.poll()
                }
                started = true
                sysSrv.onStart()
                break
            case "status":
                if(sysSrv.args.size() > 0) {
                    sysSrv.args.poll()
                }
                service.onStatus(lockFile.exists())
                break
            default:
                try {
                    Method m = service.class.getDeclaredMethod("on" + action.capitalize())
                    try {
                        if(sysSrv.args.size() > 0) {
                            sysSrv.args.poll()
                        }
                        m.invoke(sysSrv)
                    } catch (Exception e) {
                        Log.e("Exception in method: on${action.capitalize()}", e)
                    }
                } catch (NoSuchMethodException ignore) {
                    started = true
                    sysSrv.onStart()
                }
                break
        }
        if(started) {
            if(lockFile.exists()) {
                Log.v("Lock file was present... removing")
                lockFile.delete()
                sleep(Millis.SECOND_2)
            }
            Log.v("Creating lock file: "+lockFile.toString())
            lockFile << ("ok" + SysInfo.newLine)
            Log.i("Waiting for command...")
            lockFile.deleteOnExit()
            while(lockFile.exists()) {
                sleep(Millis.SECOND)
                sysSrv.onSleep()
            }
        }
        service.onStop()
        service.kill(0)
    }

    @SuppressWarnings('GrMethodMayBeStatic')
    void exit(int code = 0) {
        exitCode = code
        main("stop")
    }
    /**
     * Will exit immediately
     * @param code
     */
    void kill(int code) {
        System.exit(code)
    }
    //------------------------------ NON STATIC ---------------------------------
    public String lockFile = "service.lock"
    public final Queue<String> args = [] as Queue<String>
    /**
     * Required to implement onStart()
     */
    abstract void onStart()
    //To overrride:
    /**
     * Return which action should do.
     * In that method "lockFile" can be modified if special location is required
     * @param args
     * @return
     */
    //The very first step before starting
    void onInit() {}

    void onStop() {
        Log.i("System exiting...")
    }

    @SuppressWarnings('GrMethodMayBeStatic')
    void onStatus(boolean running) {
        Log.i("Status: "+(running ? "RUNNING" : "STOPPED"))
        System.exit(2)
    }
    void onRestart() {}
    /**
     * This method will be called each second
     * ideal to run background processes
     */
    void onSleep() {}
}
