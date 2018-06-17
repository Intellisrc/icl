package jp.sharelock.web.services

import groovy.transform.CompileStatic
import jp.sharelock.etc.Log
import jp.sharelock.web.Service
import jp.sharelock.web.ServiciableSingle
import spark.Request

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * @since 10/19/17.
 */
@CompileStatic
class LogService implements ServiciableSingle {

    @Override
    String getPath() {
        "/log"
    }

    @Override
    Service getService() {
        return new Service(
            action : {
                Request request ->
                    //specify in Log if we want to export it, then read it here and return it with parameters
                    //like: from to, date, etc... (filters)
                    def from = request.queryParams("from") ?: ""
                    def to   = request.queryParams("to") ?: ""
                    def type = request.queryParams("type") ?: ""
                    def keyword = request.queryParams("key") ?: ""
                    def res = [:]
                    if(Log.logFile) {
                        def log = new File(Log.logFile)
                        if(log.exists()) {
                            def logs = log.text.split("\n")
                            def filtered = logs.findAll {
                                String line ->
                                    def parts = line.split("\t")
                                    LocalDateTime lTime = parts.first().toDateTime()
                                    def inc = true
                                    if(from) {
                                        LocalDateTime dFrom = from.toDateTime()
                                        inc = lTime >= dFrom
                                    }
                                    if(inc && to) {
                                        LocalDateTime dTo = to.toDateTime()
                                        inc = lTime <= dTo
                                    }
                                    if(inc && keyword) {
                                        inc = line.contains(keyword)
                                    }
                                    if(inc && type) {
                                        inc = line.contains(" [" + type.toUpperCase() + "] ")
                                    }
                                    return inc
                            }
                            res = filtered.collect {
                                String line ->
                                    def parts = line.split("\t")
                                    return [
                                        time : parts.first(),
                                        type : parts[1].replace("[","").replace("]","").trim(),
                                        clss : parts[2].trim(),
                                        mssg : parts[3].trim()
                                    ]
                            }
                        }
                    }
                    return res
            } as Service.ActionRequest
        )
    }
}
