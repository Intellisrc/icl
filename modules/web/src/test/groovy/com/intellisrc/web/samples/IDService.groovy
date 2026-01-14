package com.intellisrc.web.samples

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Response
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.SingleService
import com.intellisrc.web.service.WebException

import java.util.concurrent.atomic.AtomicInteger

import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404
import static org.eclipse.jetty.http.HttpStatus.NOT_IMPLEMENTED_501
import static org.eclipse.jetty.http.HttpStatus.SERVICE_UNAVAILABLE_503

/**
 * @since 17/04/19.
 */
class IDService extends SingleService {
    AtomicInteger calls = new AtomicInteger()
    AtomicInteger errors = new AtomicInteger()
    int cacheTime = 10

    @Override
    Service.ServiceError getOnError() {
        return {
            WebException we ->
                errors.incrementAndGet()
                Log.w("Error %d found: ", we.code, we)
                return true // do not process default
        }
    }

    @Override
    Service getService() {
        return new Service(
            path : "/id/:id/",
            allowOrigin: "127.0.0.1",
            cacheTime: cacheTime,
            /* onError: {   // If set, it will call this one
                WebException we ->
                    errors.incrementAndGet()
                    Log.w("Service Error %d found: ", we.code, we)
                    return true // do not process default
            },*/
            action: {
                Request request, Response response ->
                    int id = 0
                    //Example use of Response
                    if(! request.hasPathParams()) {
                        Log.e("No parameters found")
                        response.status(NOT_FOUND_404)
                        response.redirect("/")
                    } else if(request.getPathParam("id") == "error501") {
                        response.sendError(NOT_IMPLEMENTED_501, "I forgot about this!")
                    } else if(request.getPathParam("id") == "error503") {
                        throw new WebException(SERVICE_UNAVAILABLE_503, "Simulating error")
                    } else if(request.getPathParam("id") == "boom") {
                        throw new RuntimeException("boom!")
                    } else {
                        calls.incrementAndGet()
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

