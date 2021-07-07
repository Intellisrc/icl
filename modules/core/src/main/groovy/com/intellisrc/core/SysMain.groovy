package com.intellisrc.core

import groovy.transform.CompileStatic

import java.lang.reflect.Method

/**
 * This class is similar to SysService but it doesn't
 * require start/stop
 *
 * @see SysService : for documentation
 *
 * @since 18/06/30.
 */
@CompileStatic
abstract class SysMain {
    static public SysMain main
    static public int exitCode = 0
    static public boolean exitJava = true //turn false to prevent System.exit() on exit() : used for testing

    static void main(String[] args) {
        if(main == null) {
            String cfgMain = Config.get("main.class", Config.system.get("main.class"))
            if(cfgMain) {
                try {
                    Class c = Class.forName(cfgMain)
                    main = c.getConstructor().newInstance() as SysMain
                } catch(Exception e) {
                    Log.e("Unable to initialize main class", e)
                }
            } else {
                Log.e("`main` is not defined. To define it, set it in 'config.properties' e.g. 'main.class = org.example.app.MyClass' or use: \n static { main = new MyClass() }\n inside MyClass")
                System.exit(3)
            }
        }
        Version.mainClass = main.class
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
        exit(0)
    }

    static void exit(int code = 0) {
        exitCode = code
        main.onStop()
        if(exitJava) {
            System.exit(code)
        }
    }
    //------------------------------ NON STATIC ---------------------------------
    public final Queue<String> args = [] as Queue<String>
    /**
     * Required to implement onStart()
     */
    abstract void onStart()
    /**
     * (Optional) what to do when system exits
     */
    @SuppressWarnings('GrMethodMayBeStatic')
    void onStop() {
        Log.i("System exiting...")
    }
}
