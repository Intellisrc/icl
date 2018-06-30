package com.intellisrc.web

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification
/**
 * @since 17/12/13.
 */
class JSONTest extends Specification {

    /**
     * JSON encoding Tests
     */
    def "Encoding"() {
        expect:
        def str = "hello!!"
        assert JSON.encode(str) == JsonOutput.toJson(str)
        def html = "<h1>H11</h1>"
        assert JSON.decode(JSON.encode(html)).toString() == new JsonSlurper().parseText(JsonOutput.toJson(html))
        def js = "function() { var x = '1000'; }"
        assert JSON.decode(JSON.encode(js)).toString() == new JsonSlurper().parseText(JsonOutput.toJson(js))
        def num = 1000
        assert JSON.encode(num) == JsonOutput.toJson(num)
        assert JSON.decode(JSON.encode(num)).toInteger() == num
        def dbl = 999.99
        assert JSON.encode(dbl) == JsonOutput.toJson(dbl)
        assert JSON.decode(JSON.encode(dbl)).toDouble() == dbl
        def lng = 300000L
        assert JSON.encode(lng) == JsonOutput.toJson(lng)
        assert JSON.decode(JSON.encode(lng)).toLong() == lng
        def bool = true
        assert JSON.encode(bool) == JsonOutput.toJson(bool)
        assert JSON.decode(JSON.encode(bool)).toBoolean() == bool
        def arr = [1,2,3,4,5,6]
        assert JSON.encode(arr) == JsonOutput.toJson(arr)
        println JSON.encode(arr)
        def arrs = ["a","b",'c']
        assert JSON.encode(arrs) == JsonOutput.toJson(arrs)
        def map = [a:1,b:2,c:3]
        assert JSON.encode(map) == JsonOutput.toJson(map)
        println JSON.encode(map)
        def maplist = [a:[1,2,3],b:[4,5,6]]
        assert JSON.encode(maplist) == JsonOutput.toJson(maplist)
        def listmap = [[a:1],[b:2],[c:3]]
        assert JSON.encode(listmap) == JsonOutput.toJson(listmap)
        def obj = "http://localhost".toURL()
        assert JSON.encode(obj) == JsonOutput.toJson(obj)
        println JSON.encode(obj)
        def date = "2017-01-01".toDate()
        assert JSON.decode(JSON.encode(date)).toDate().YMD == date.YMD
    }
    /**
     * JSON decoding tests
     * @return
     */
    def "Decoding"() {
        setup:

        def obj = [
            user : 100,
            name : "G. Lucas",
            dates : [
                "2018-02-12".toDate(),
                "2017-10-14 10:05:34".toDateTime(),
                "10:30:10".toTime()
            ]
        ]

        expect:

        def jsonStr = JSON.encode(obj)
        println jsonStr
        def map = JSON.decode(jsonStr).toMap()
        assert map.user == obj.user
        assert map.name == obj.name
        def list = map.dates as List
        assert list.size() == obj.dates.size()
        assert list[0].toString().toDate().YMD == obj.dates[0].YMD
        assert list[1].toString().toDateTime().YMDHms == obj.dates[1].YMDHms
        assert list[2].toString().toTime().HHmmss == obj.dates[2].HHmmss
    }
    /**
     * Test pretty output
     */
    def "Pretty pring"() {
        setup:

        def obj = [
                    user : 100,
                    name : "G. Lucas",
                    address : [
                            state  : "Water State",
                            city   : "Fire City",
                            street : "Main Rd.",
                            place  : [
                                    building : "Sun Terrace",
                                    number : 303
                            ]
                    ]
                ]
        def jsonStr = JSON.encode(obj)
        def prettyStr = JSON.encode(obj, true)

        expect:
        assert jsonStr != prettyStr
        println prettyStr
    }
}
