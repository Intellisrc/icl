package com.intellisrc.etc

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import java.lang.reflect.Method

/**
 * This class is similar to SysService but it doesn't
 * require start/stop
 * @since 18/06/30.
 */
@CompileStatic
abstract class SysMain {
    static SysMain main
    static int exitCode = 0
    static void main(String[] args) {
        if(main == null) {
            Log.e("`main` is not defined. To define it, use: \n static {\n     main = new MyClass()\n }\n inside MyClass")
            System.exit(3)
        }
        //Process args before starting
        main.args.addAll(args.toList())
        if(main.args.empty) {
            main.onStart()
        } else {
            String action = main.args.first()
            try {
                Method m = main.class.getDeclaredMethod("on" + action.capitalize())
                try {
                    if (main.args.size() > 0) {
                        main.args.poll()
                    }
                    m.invoke(main)
                } catch (Exception e) {
                    Log.e("Exception in method: on${action.capitalize()}", e)
                }
            } catch (NoSuchMethodException e) {
                main.onStart()
            }
        }
        //When onStart() finish, call onStop(), then exit
        main.onStop()
        System.exit(0)
    }
    void exit(int code = 0) {
        exitCode = code
        main.onStop()
    }
    //------------------------------ NON STATIC ---------------------------------
    final Queue<String> args = [] as Queue<String>
    /**
     * Required to implement onStart()
     */
    abstract void onStart()
    /**
     * (Optional) what to do when system exits
     */
    void onStop() {
        Log.i("System exiting...")
        System.exit(0)
    }
}
