package com.intellisrc.web.service

import groovy.transform.CompileStatic

@CompileStatic
class UploadFile extends File {
    final String originalName
    final String inputName

    UploadFile(String filePath, String origName, String input) {
        super(filePath)
        originalName = origName
        inputName = input
    }
}