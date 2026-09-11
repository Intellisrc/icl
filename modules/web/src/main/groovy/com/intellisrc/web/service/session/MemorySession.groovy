package com.intellisrc.web.service.session

import groovy.transform.CompileStatic
import org.eclipse.jetty.session.NullSessionDataStore

/**
 * Keeps the sessions in memory only (Jetty default behavior).
 * This is the default session store: sessions are lost when the
 * WebService is restarted.
 *
 * @since 2026/09/11.
 */
@CompileStatic
class MemorySession extends NullSessionDataStore implements SessionPersistable {
    @Override
    String name() {
        return "memory"
    }
}
