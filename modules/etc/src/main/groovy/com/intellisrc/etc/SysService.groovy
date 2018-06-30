package com.intellisrc.etc

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
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
    static SysService service
    static int exitCode = 0
    static void main(String[] args) {
        if(service == null) {
            Log.e("`service` is not defined. To define it, use: \n static {\n     service = new MyClass()\n }\n inside MyClass")
            System.exit(3)
        }
        def sysSrv = service
        //Process args before starting
        sysSrv.args.addAll(args.toList())
        //Initialize
        sysSrv.onInit()
        String action = sysSrv.args.isEmpty() ? "start" : sysSrv.args.first()
        def usrDir = SysInfo.getWritablePath()
        def lockFile = new File(usrDir + sysSrv.lockFile)
        boolean startService = false
        switch(action) {
            case "stop" :
                Log.v("Removing lock file")
                lockFile.delete()
                System.exit(exitCode)
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
                startService = true
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
                } catch (NoSuchMethodException e) {
                    startService = true
                    sysSrv.onStart()
                }
                break
        }
        if(startService) {
            if(lockFile.exists()) {
                Log.v("Lock file was present... removing")
                lockFile.delete()
                sleep(2000)
            }
            Log.v("Creating lock file: "+lockFile.toString())
            lockFile << "ok\n"
            Log.i("Waiting for command...")
            lockFile.deleteOnExit()
            while(lockFile.exists()) {
                sleep(1000)
                sysSrv.onSleep()
            }
        }
        service.onStop()
        System.exit(0)
    }
    void exit(int code = 0) {
        exitCode = code
        main("stop")
    }
    //------------------------------ NON STATIC ---------------------------------
    String lockFile = "service.lock"
    final Queue<String> args = [] as Queue<String>
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
        System.exit(0)
    }
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
