package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.web.Request
import com.intellisrc.web.Response
import com.intellisrc.web.Service
import com.intellisrc.web.SingleService

/**
 * @since 17/04/19.
 */
class IDService extends SingleService {
    Service getService() {
        return new Service(
            path : "/id/:id/?",
            cacheTime: 10,
            cacheExtend: true,
            action: {
                Request request, Response response ->
                    int id = 0
                    //Example use of Response
                    if(! request.hasPathParams()) {
                        Log.e("No parameters found")
                        response.status(404)
                        response.redirect("/")
                    } else {
                        id = request.getPathParam("id") as Integer
                        Log.v("ID requested: %d", id)
                    }
                return [
                    i : id,
                    t : SysClock.dateTime.toLocalTime().HHmmss
                ]
            }
        )
    }
}

