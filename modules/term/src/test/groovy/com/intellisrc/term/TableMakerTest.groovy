package com.intellisrc.term

import spock.lang.Specification

/**
 * @since 2022/04/08.
 */
class TableMakerTest extends Specification {
    def "List of List to Print"() {
        setup:
            List<List<String>> data = []
            if(header) {
                data << ["Fruit", "QTY", "Price", "Seller"]
            }
            data.addAll([
                ["Apple","1000","10.00","some@example.com"],
                ["Banana","2002","15.00","anyone@example.com"],
                ["Mango","400","134.10","dummy200@example.com"],
                ["Kiwi","900","2350.40","example@example.com"]
            ])
            if(footer) {
                data << ["Fruits: 4", "Sum: 4302", "Total: 2509.5", ""]
            }
            TableMaker tp = new TableMaker(data, header, footer)
        when:
            println "[ Header : " + header + " / Footer : " + footer + " ]"
            tp.print()
        then:
            assert tp.toString().contains("Apple")
            assert tp.toString().contains("Banana")
            assert tp.toString().contains("Mango")
            assert tp.toString().contains("Kiwi")
        where:
            header  | footer
            true    | true
            true    | false
            false   | true
            false   | false
    }
    def "List of Map to Print"() {
        setup:
            List<Map<String, String>> data = [] as List<Map<String, String>>
            data.add([
                Fruit   : "Apple",
                QTY     : "1000",
                Price   : "10.00",
                Seller  : "some@example.com"
            ] as Map<String,String>)
            data.add([
                Fruit   : "Banana",
                QTY     : "2002",
                Price   : "15.00",
                Seller  : "anyone@example.com"
            ] as Map<String,String>)
            data.add([
                Fruit   : "Mango",
                QTY     : "400",
                Price   : "134.10",
                Seller  : "dummy200@example.com"
            ] as Map<String,String>)
            data.add([
                Fruit   : "Kiwi",
                QTY     : "900",
                Price   : "2350.40",
                Seller  : "example@example.com"
            ] as Map<String,String>)
            if(footer) {
                data << [
                    Fruit   : "Fruits: 4",
                    QTY     : "Sum: 4302",
                    Price   : "Total: 2509.5",
                    Seller  : ""
                ]
            }
            TableMaker tp = new TableMaker(data, footer)
        when:
            println "[ " +  "Footer : " + footer + " ]"
            tp.print()
        then:
            assert tp.toString().contains("Apple")
            assert tp.toString().contains("Banana")
            assert tp.toString().contains("Mango")
            assert tp.toString().contains("Kiwi")
        where:
            footer  | unused
            true    | 0
            false   | 0
    }
    def "Using main constructor an expandible footer"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Name","Email","Age"],
                footer: ["All names here are fictitious"]
            )
        when:
            tp << ["Joshep Patrishius", "jp@example.com", "41"]
            tp.addRow(["Marilin Watson", "marilin-watson1023@example.com", "19"])
            tp << ["Raphael Kawami", "kawami-rapha@example.com", "33"]
            tp.addRow(["Zoe Mendoza", "you-know-who@example.com", "54"])
        then:
            tp.print()
            assert !tp.rows.empty
    }
}
