package com.intellisrc.crypt.hash

import groovy.transform.CompileStatic

/**
 * @since 17/04/07.
 */
@CompileStatic
interface Hashable {
    static interface hashType {}
    void setCost(int costSet)
    String hash(String algorithm)
    boolean verify(String hash, String algorithm)
}