package jp.sharelock.srv

/**
 * Update system and data
 * @since 1/2/17.
 */
@groovy.transform.CompileStatic
abstract class UpdaterA {
    String remote;
    String local_path;
    /**
     * Updates the system libraries and binaries
     * @return
     */
    boolean updateSystem(){

    }
    /**
     * Updates black list getting the last N items
     * @param items
     * @return
     */
    boolean updateBlackList(int items) {

    }
}
