package jp.sharelock.crypt.hash

/**
 * @since 17/04/07.
 */
@groovy.transform.CompileStatic
interface Hashable {
    interface hashType {}
    void setCost(int costSet)
    String hash(String algorithm)
    boolean verify(String hash, String algorithm)
}