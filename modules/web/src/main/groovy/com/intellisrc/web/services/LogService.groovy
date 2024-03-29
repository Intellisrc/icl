package com.intellisrc.web.services

import com.intellisrc.core.Config
import com.intellisrc.web.service.Request
import com.intellisrc.web.service.Response
import com.intellisrc.web.service.Service
import com.intellisrc.web.service.ServiciableSingle
import groovy.transform.CompileStatic

import java.time.LocalDateTime

/**
 * @since 10/19/17.
 */
@CompileStatic
class LogService implements ServiciableSingle {
    final File logDir = Config.getFile("log.dir", "log")
    final String logFileName = Config.get("log.file.name", "system.log")

    @Override
    String getPath() {
        "/log"
    }

    @Override
    Service getService() {
        return new Service(
            action : {
                Request request, Response response ->
                    //specify in Log if we want to export it, then read it here and return it with parameters
                    //like: from to, date, etc... (filters)
                    def from = request.queryParams("from") ?: ""
                    def to   = request.queryParams("to") ?: ""
                    def type = request.queryParams("type") ?: ""
                    def clss = request.queryParams("class") ?: ""
                    def meth = request.queryParams("method") ?: ""
                    def keyword = request.queryParams("key") ?: ""
                    def res = [:]
                    File file = from ? new File(logDir, from.split(" ")[0].toDate().getYMD("-") + "-" + logFileName)
                                     : new File(logDir, logFileName)
                    if(file.exists()) {
                        def logs = file.text.split("\n")
                        res = logs.findAll {
                            String line ->
                                def parts = line.split("\t")
                                LocalDateTime lTime = parts.first().toDateTime()
                                String
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
                                    inc = parts[4].toLowerCase().contains(keyword.toLowerCase())
                                }
                                if(inc && clss) {
                                    inc = parts[2].toLowerCase().contains(clss.toLowerCase())
                                }
                                if(inc && meth) {
                                    inc = parts[3].toLowerCase().contains(meth.toLowerCase())
                                }
                                if(inc && type) {
                                    inc = parts[1].contains(type.toUpperCase())
                                }
                                return inc
                        }.collect {
                            String line ->
                                def parts = line.split("\t")
                                return [
                                    time : parts.first(),
                                    type : parts[1].replace("[","").replace("]","").trim(),
                                    clss : parts[2].trim(),
                                    meth : parts[3].trim(),
                                    mssg : parts[4].trim()
                                ]
                        }
                    }
                    return res
            }
        )
    }
}
