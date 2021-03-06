package com.intellisrc.term

import com.intellisrc.core.AnsiColor
import com.intellisrc.term.styles.BoldStyle
import com.intellisrc.term.styles.ClassicStyle
import com.intellisrc.term.styles.SafeStyle
import com.intellisrc.term.styles.DoubleLineStyle
import com.intellisrc.term.styles.SemiDoubleStyle
import com.intellisrc.term.styles.ThinStyle
import spock.lang.Specification

import static com.intellisrc.core.AnsiColor.*
import static com.intellisrc.term.TableMaker.Align.*
import static com.intellisrc.term.TableMakerAnswers.*

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
            tp.style = style
            tp.compact = compact
            tp.print()
        then:
            assert tp.toString().trim() == answer
        where:
            header  | footer | style                    | compact   | answer
            true    | true   | new SafeStyle()          | true      | answer1
            true    | false  | new BoldStyle()          | false     | answer2
            false   | true   | new DoubleLineStyle()    | true      | answer3
            false   | false  | new ThinStyle()          | false     | answer4
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
            tp.style = new SemiDoubleStyle()
            tp.print()
        then:
            assert tp.toString().trim() == answer
        where:
            footer  | answer
            true    | answer5
            false   | answer6
    }
    def "Using main constructor an expandable footer"() {
        setup:
            TableMaker tp = new TableMaker(
                headers : ["Name","Email","Age"],
                footer  : ["All names here are fictitious"],
                compact : true,
                style   : new ClassicStyle()
            )
        when:
            tp << ["Joshep Patrishius", "jp@example.com", "41"]
            tp.addRow(["Marilin Watson", "marilin-watson1023@example.com", "19"])
            tp << ["Raphael Kawami", "kawami-rapha@example.com", "33"]
            tp.addRow(["Zoe Mendoza", "you-know-who@example.com", "54"])
        then:
            tp.print()
            assert tp.toString().trim() == answer7
    }
    def "Changing column properties"() {
        setup:
            TableMaker tp = new TableMaker(
                headers : ["Name","User Email Address", "Cost"],
                footer  : ["All names here are fictitious"],
                compact : true,
                style   : new ClassicStyle()
            )
        when:
            tp << ["Joshep Patrishius", "jp@example.com", 41]
            tp.addRow(["Marilin Watson", "marilin-watson1023@example.com", 1339])
            tp << ["Raphael Kawami", "kawami-rapha@example.com", 3893]
            tp.addRow(["Zoe Mendoza", "you-know-who@example.com", 1999254])
            tp.columns[1].maxLen = 10
            tp.columns[1].ellipsis = true
            tp.columns[2].align = RIGHT
            tp.columns[2].headerColor = YELLOW
            tp.columns[2].formatter = {
                    String.format("\$ %.2f", it as double)
            }
            tp.columns[2].color = { it > 2000 ? RED : GREEN }
        then:
            tp.print()
            String out = tp.toString().trim()
            assert decolor(out) == answer8
            assert out.contains(RED)
            assert out.contains(GREEN)
            assert out.contains(YELLOW)
    }
    def "Using setRows"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 10.00, "some@example.com"],
                    ["Banana", 2002, 15.00, "anyone@example.com"],
                    ["Mango", 400, 134.10, "dummy200@example.com"],
                    ["Kiwi", 900, 2350.40, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer9
    }
    def "Ellipsis should show at the left when using align right"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 10.00, "some@example.com"],
                    ["Banana", 2002, 15.00, "anyone@example.com"],
                    ["Mango", 400, 134.10, "dummy200@example.com"],
                    ["Kiwi", 900, 2350.40, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[3].with {
                maxLen = 12
                align = RIGHT
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer10
    }
    def "centering text should display nicely"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 10.00, "some@example.com"],
                    ["Banana", 2002, 15.00, "a@example.com"],
                    ["Mango", 400, 134.10, "very_long_dummy200@example.com"],
                    ["Kiwi", 900, 2350.40, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[3].with {
                align = CENTER
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer11
    }
    def "Ellipsis should show on both sides when using align = center"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 10.00, "some@example.com"],
                    ["Banana", 2002, 15.00, "a@example.com"],
                    ["Mango", 400, 134.10, "very_long_dummy200@example.com"],
                    ["Kiwi", 900, 2350.40, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[3].with {
                maxLen = 15
                align = CENTER
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer12
    }
    def "minLen should be taken into account"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 101, "some@example.com"],
                    ["Banana", 2002, 150, "a@example.com"],
                    ["Mango", 400, 134, "very_long_dummy200@example.com"],
                    ["Kiwi", 900, 235, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[2].with {
                minLen = 10
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer13
    }
    def "Combining minLen and maxLen"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 1, "some@example.com"],
                    ["Banana", 2002, 150, "a@example.com"],
                    ["Mango", 400, 1344, "very_long_dummy200@example.com"],
                    ["Kiwi", 900, 23, "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[2].with {
                minLen = 1
                maxLen = 4
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer14
    }

    def "hideColumn should not display column if empty"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, "", "some@example.com"],
                    ["Banana", 2002, "", "a@example.com"],
                    ["Mango", 400, "", "very_long_dummy200@example.com"],
                    ["Kiwi", 900, "", "example@example.com"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[2].with {
                hideWhenEmpty = true
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer15
    }
    def "autoCollapse should reduce column sizes"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, 1234, ""],
                    ["Banana", 2002, 52233, ""],
                    ["Mango", 400, 164643, "very_long_dummy200@example.com"],
                    ["Kiwi", 500, 91827161, ""],
                    ["Orange", 800, 456456, ""],
                    ["Papaya", 900, 23774, ""],
                    ["Yuzu", 100, 22, ""],
                    ["Strawberries", 300, 556, ""],
                    ["Watermelon", 7900, 1, ""],
                    ["Melon", 700, 678, ""],
                    ["Lemon", 900, 553, ""],
                    ["Guava", 2000, 12678, ""],
                    ["Grapes", 700, 21, ""],
                    ["Lime", 5400, 235, ""]
                ],
                footer: "Fruits: 4"
            )
            tp.columns.each {
                it.autoCollapse = true
                it.minLen = 4
            }
        when:
            tp.print()
        then:
            assert tp.toString().trim() == answer16
    }
    def "Trimming with colors should work fine"() {
        setup:
            TableMaker tp = new TableMaker(
                headers: ["Fruit", "QTY", "Price", "Seller"],
                rows: [
                    ["Apple", 1000, "", "${RED}some${RESET}@${GREEN}example.com${RESET}"],
                    ["Banana", 2002, "", "${RED}a${RESET}@${GREEN}example.com${RESET}"],
                    ["Mango", 400, "", "${RED}very_long_dummy200${RESET}@${GREEN}example.com${RESET}"],
                    ["Kiwi", 900, "", "${RED}example${RESET}@${GREEN}example.com${RESET}"]
                ],
                footer: "Fruits: 4"
            )
            tp.columns[3].with {
                maxLen = len
                align = al
            }
        when:
            tp.print()
        then:
            assert true //TODO: test colors
        where:
            len         | al
            10          | LEFT
            15          | LEFT
            20          | LEFT
            25          | LEFT
            30          | LEFT
            10          | RIGHT
            15          | RIGHT
            20          | RIGHT
            25          | RIGHT
            30          | RIGHT
            10          | CENTER
            15          | CENTER
            20          | CENTER
            25          | CENTER
            30          | CENTER
    }
}
