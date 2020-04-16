package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.web.Service
import com.intellisrc.web.ServiciableSingle
import com.intellisrc.core.SysClock
import spark.Request
import spark.Response


/**
 * @since 17/04/19.
 */
class IDService implements ServiciableSingle {
    Service getService() {
        return new Service(
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
                    t : SysClock.dateTime.toLocalTime().HHmmss
                ]
            } as Service.Action
        )
    }

    String getPath() {
        return "/id"
    }
}

