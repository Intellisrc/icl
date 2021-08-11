package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import com.intellisrc.etc.Mime
import com.intellisrc.web.Service
import com.intellisrc.web.ServiciableMultiple
import com.intellisrc.web.UploadFile

import static com.intellisrc.web.Service.Method.*

/**
 * @since 11/30/17.
 */
class UploadService implements ServiciableMultiple {
    final File uploadDir
    UploadService(File upDir) {
        uploadDir = upDir
    }

    @Override
    List<Service> getServices() {
        return [
            new Service(
                path: "/check",
                contentType: "text/plain",
                action: {
                    return "ok"
                }
            ),
            new Service(
                path: "/upload",
                method: POST,
                contentType: Mime.getType("gif"),
                action: {
                    UploadFile upFile ->
                        Log.i("File uploaded : %s", upFile.originalName)

                        // Example on how to move the uploaded file into a specific directory:
                        File dstFile = SysInfo.getFile(uploadDir, upFile.originalName)
                        if(dstFile.exists()) { dstFile.delete() }
                        upFile.moveTo(dstFile)

                        return dstFile
                }
            )
        ]
    }
}
