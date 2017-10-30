package jp.sharelock.etc

import groovy.transform.CompileStatic

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
 */
@CompileStatic
abstract class SysService {
    static SysService service
    static void main(String[] args) {
        if(service == null) {
            Log.e("`service` is not defined. To define it, use: \n static {\n     service = new MyClass()\n }\n inside MyClass")
            System.exit(3)
        }
        def sysSrv = service
        //Process args before starting
        def action = sysSrv.getAction(args.toList())
        def usrDir = SysInfo.getWritablePath()
        def lockFile = new File(usrDir + sysSrv.lockFile)
        if(!action) {
            action = "start"
        }
        Log.i("Service Action : " + action)
        switch(action) {
            case "stop" :
                Log.d("Removing lock file")
                lockFile.delete()
                System.exit(0)
                break
            case "restart":
                sysSrv.onRestart()
                //no break
            case "start":
                if(lockFile.exists()) {
                    Log.d("Lock file was present... removing")
                    lockFile.delete()
                    sleep(2000)
                }
                Log.d("Creating lock file: "+lockFile.toString())
                lockFile << "ok\n"
                sysSrv.onStart()
                break
            case "status":
                sysSrv.onStatus(lockFile.exists())
                break
            case "reload":
                sysSrv.onReload()
                break
            case "install":
                sysSrv.onInstall()
                break
            case "uninstall":
                sysSrv.onUninstall()
                break
            default:
                Log.e("Unknown command : "+action)
                System.exit(1)
                break
        }
        Log.i("Waiting for command...")
        lockFile.deleteOnExit()
        while(lockFile.exists()) {
            sleep(1000)
            sysSrv.onSleep()
        }
        sysSrv.onStop()
        System.exit(0)
    }
    //------------------------------ NON STATIC ---------------------------------
    String lockFile = "service.lock"
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
    String getAction(List<String> args) { return args ? args.first() : "" }
    void onStop() {
        Log.i("System exiting...")
        System.exit(0)
    }
    void onStatus(boolean running) {
        Log.i("Status: "+(running ? "RUNNING" : "STOPPED"))
        System.exit(2)
    }
    void onRestart() {}
    void onReload() {
        Log.w("'reload' is not implemented")
        System.exit(2)
    }
    void onInstall() {
        Log.w("'install' is not implemented")
        System.exit(2)
    }
    void onUninstall() {
        Log.w("'uninstall' is not implemented")
        System.exit(2)
    }
    /**
     * This method will be called each second
     * ideal to run background processes
     */
    void onSleep() {}
}
