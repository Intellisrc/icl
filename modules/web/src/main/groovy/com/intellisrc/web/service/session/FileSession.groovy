package com.intellisrc.web.service.session

import com.intellisrc.core.Config
import groovy.transform.CompileStatic
import org.eclipse.jetty.session.FileSessionDataStore

/**
 * Persists the sessions as files in a directory (using the Jetty
 * FileSessionDataStore), so they survive a restart of the WebService.
 *
 * The directory can be specified with web.session.dir (default: ".sessions").
 * Each running WebService instance must use a different directory.
 *
 * NOTE: the values stored as session attributes must be Serializable
 * to be able to survive a restart.
 *
 * @since 2026/09/11.
 */
@CompileStatic
class FileSession extends FileSessionDataStore implements SessionPersistable {
    protected static final String defaultSessionDir = Config.any.get("web.session.dir", ".sessions")

    /**
     * @param storeDir : Directory where the sessions are stored.
     *                  Default: web.session.dir (or ".sessions")
     */
    FileSession(File storeDir = null) {
        File dir = storeDir ?: File.get(defaultSessionDir)
        if(!dir.exists()) {
            dir.mkdirs()
        }
        setStoreDir(dir)
        // Delete corrupt files instead of failing to start:
        setDeleteUnrestorableFiles(true)
    }

    @Override
    String name() {
        return "file"
    }
}
