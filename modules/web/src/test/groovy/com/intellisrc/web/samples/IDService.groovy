package com.intellisrc.web.samples

import com.intellisrc.etc.Log
import spark.Request
import spark.Response

/**
 * @since 17/04/19.
 */
class IDService implements com.intellisrc.web.ServiciableSingle {
    com.intellisrc.web.Service getService() {
        return new com.intellisrc.web.Service(
            cacheTime: 10,
            cacheExtend: true,
            action: {
                Request request, Response response ->
                    int id = 0
                    //Example use of Response
                    if(request.queryParams().isEmpty()) {
                        Log.e("No parameters found")
                        response.status(404)
                        response.redirect("/")
                    } else {
                        id = request.queryParams("i") as Integer
                        Log.v("ID requested: %d", id)
                    }
                return [
                    i : id,
                    t : new Date().time
                ]
            } as com.intellisrc.web.Service.ActionRequestResponse
        )
    }

    String getPath() {
        return "/id"
    }
}

