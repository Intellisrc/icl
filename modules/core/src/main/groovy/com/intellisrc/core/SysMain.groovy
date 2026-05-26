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
                Log.e("`main` is not defined. To define it, set it in 'config.properties' " +
                    "e.g. 'main.class = org.example.app.MyClass' or use: " + SysInfo.newLine +
                    " static { main = new MyClass() }" + SysInfo.newLine +
                    " inside MyClass")
                System.exit(3)
            }
        }
        Version.mainClass = main.class
        //Process args before starting
        main.args.addAll(args.toList())
        // Use the helper to attempt dynamic invocation
        if (!invokeActionMethod(main, main.args)) {
            // Fallback if args were empty or the method didn't exist
            main.onStart()
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

    /**
     * Attempts to dynamically invoke a public 'onAction' method on an instance.
     * If the method exists, the action argument is consumed and the method is executed.
     * * @return true if a method was found and executed; false if NoSuchMethodException occurred.
     */
    static boolean invokeActionMethod(Object instance, Queue<String> args) {
        if (args.isEmpty()) return false

        String action = args.first()
        String methodName = "on" + action.capitalize()

        try {
            // Looks up public methods in the target class AND all superclasses
            Method m = instance.class.getMethod(methodName)

            // Consume the action name from arguments before execution
            args.poll()
            m.invoke(instance)
            return true
        } catch (NoSuchMethodException ignore) {
            return false // Let the caller fall back to default behavior (e.g., onStart())
        } catch (Exception e) {
            Log.e("Exception in method: ${methodName}", e)
            return true // The method existed but crashed; we consider it handled
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
