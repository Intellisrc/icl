package com.intellisrc.crypt.hash

import groovy.transform.CompileStatic

/**
 * @since 2021/07/07.
 */
@CompileStatic
enum HashType {
    BCRYPT, SCRYPT, PBKDF2
    String getCryptHeader() {
        String header = ""
        switch(this) {
            case BCRYPT: header = '$2y$'; break
            case SCRYPT: header = '$s0$'; break
        }
        return header
    }
    static HashType fromString(String stype) {
        return stype.toUpperCase() as HashType
    }
}