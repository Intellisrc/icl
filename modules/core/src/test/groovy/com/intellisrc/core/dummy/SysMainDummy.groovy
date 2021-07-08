package com.intellisrc.core.dummy

import com.intellisrc.core.SysMain

/**
 * @since 2021/07/07.
 */
class SysMainDummy extends SysMain {
    static boolean executed = false
    @Override
    void onStart() {
        executed = true
    }
}
