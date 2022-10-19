package com.intellisrc.term

import com.intellisrc.core.Log
import com.intellisrc.term.styles.SafeStyle
import com.intellisrc.term.styles.Stylable
import com.intellisrc.term.utils.CharWidthMetrics
import groovy.transform.CompileStatic

import java.util.regex.Matcher

import static com.intellisrc.core.AnsiColor.*
import static com.intellisrc.term.TableMaker.Align.*

/**
 * Will print data as table in the terminal
 * @since 2022/04/08.
 */
@CompileStatic
class TableMaker {
    /**
     * It is used to give format to each cell
     */
    static interface Formatter {
        String call(Object cell)
    }
    /**
     * Column alignment
     */
    static enum Align {
        LEFT, CENTER, RIGHT
    }
    /**
     * A row is a list of cells
     */
    static class Row {
        Collection cells = []
    }
    /**
     * Information and format of a column
     */
    static class Column {
        int length = 0 // The calculated length of the column
        int minLen = 1 // Minimum length of column
        int maxLen = 0 // The maximum length allowed (user input)
        boolean ellipsis = true
        boolean expandFooter = false // Single cell row
        boolean autoCollapse = false // Reduce column width as much as possible
        boolean hideWhenEmpty = true // Do not show column if has no data
        Align align = LEFT
        Formatter formatter = { Object it -> return it.toString() } as Formatter
        Formatter color = { Object it -> return "" } as Formatter // Return AnsiColor.* to color cell
        Object header = ""
        Object footer = ""
        String headerColor = ""
        String footerColor = ""
        String ellipsisChar = "…"
        int getMaxLen() {
            return this.maxLen ?: this.length
        }
        String getFmtHeader() {
            return headerColor + trimPad(header.toString()) + (headerColor ? RESET : "")
        }
        String getFmtFooter() {
            return footerColor + trimPad(footer.toString()) + (footerColor ? RESET : "")
        }
        String pad(String text, int len = getMaxLen()) {
            String padded = text
            if(len) {
                switch (align) {
                    case LEFT:   padded = padRight(padded, len); break
                    case RIGHT:  padded = padLeft(padded, len); break
                    case CENTER:
                        int half = Math.floor((len - getDisplayWidth(text)) / 2d) as int
                        if(half > 0) {
                            padded = padRight(padLeft(padded, getDisplayWidth(text) + half), len)
                        }
                        break
                }
            }
            return padded
        }
        String trim(String text, boolean useEllipsis = ellipsis, int len = getMaxLen()) {
            String trimmed = text
            if(len && getDisplayWidth(text) > len) {
                switch (align) {
                    case LEFT:
                        trimmed = trimDisplayLeft(text, useEllipsis, ellipsisChar, len)
                        break
                    case RIGHT:
                        trimmed = trimDisplayRight(text, useEllipsis, ellipsisChar, len)
                        break
                    case CENTER:
                        trimmed = trimDisplayBoth(text, useEllipsis, ellipsisChar, len)
                        break
                }
            }
            return trimmed
        }
        /**
         * It will cut and pad the cell.
         * If the cell has colors in it it will place them back after trimming the string
         * @param text
         * @param len
         * @return
         */
        String trimPad(String text, int len = getMaxLen()) {
            boolean colored = hasColor(text)
            List<Map> info = []
            String original = decolor(text)
            if(colored) {
                Matcher match = (text =~ colorRegex)
                while (match.find()) {
                    info << [ c : match.group(), i : match.start() ]
                }
                text = decolor(text)
            }
            String after = trim(pad(text, len), ellipsis, len)
            if(colored) {
                Closure addColor = {
                    int offset ->
                        info.each {
                            try {
                                after = after.insertAt((it.i as int) + offset, it.c.toString())
                            } catch (Exception ignore) {} // It means it falls out of the string after being trimmed
                        }
                }
                switch (align) {
                    case LEFT:
                        addColor(0)
                        break
                    case RIGHT:
                        // If spaces were added...
                        if(getDisplayWidth(original.trim()) == getDisplayWidth(after.trim())) {
                            addColor(after.indexOf(original.trim()))
                        } else {
                            String sub = (26 as char).toString()
                            after = padLeft(after, getDisplayWidth(original), sub)
                            addColor(0)
                            after = after.replace(sub, "")
                        }
                        break
                    case CENTER:
                        int half = (Math.floor((getDisplayWidth(original) - getDisplayWidth(after)) / 2d) as int) - 1 //TODO: not sure why -1, but it works
                        // If spaces were added...
                        if(getDisplayWidth(original.trim()) == getDisplayWidth(after.trim())) {
                            addColor(after.indexOf(original.trim()))
                        } else { // The string was cut
                            String sub = "*" //(26 as char).toString()
                            after = padRight(after, getDisplayWidth(original) - half, sub)
                            after = padLeft(after, getDisplayWidth(original), sub)
                            addColor(0)
                            after = after.replace(sub, "")
                        }
                        break
                }
                if(! after.endsWith(RESET)) {
                    after += RESET
                }
            }
            return after
        }
        String format(Object cell, boolean trim = true) {
            String color = color.call(cell)
            String formatted = formatter.call(cell)
            return color + (trim ? trimPad(formatted) : formatted) + (color ? RESET : "")
        }
    }

    // Style used to generate table
    Stylable style = new SafeStyle()
    String borderColor = ""
    List<Column> columns = []
    final List<Row> rows = []
    boolean compact = false

    /**
     * Default constructor. Data will be passed through methods
     */
    TableMaker() {}
    /**
     * Import List<Map> into a table
     * @param data
     * @param footer
     */
    TableMaker(List<Map> data, boolean footer = false) {
        setHeaders(data.first().keySet().toList())
        data.each {
            Map entry ->
                if(footer && entry == data.last()) {
                    setFooter(entry.values().toList())
                } else {
                    addRow(entry.values().toList())
                }
        }
    }
    /**
     * Import List<List> into a table
     * @param data
     * @param headers
     * @param footer
     */
    TableMaker(List<List> data, boolean headers, boolean footer) {
        if(headers) {
            setHeaders(data.pop())
        }
        data.each {
            List entry ->
                if(footer && entry == data.last()) {
                    setFooter(entry)
                } else {
                    addRow(entry)
                }
        }
    }
    /**
     * Set headers of table
     * @param heads
     */
    void setHeaders(Collection heads) {
        if(columns.empty) {
            columns.addAll(heads.collect { new Column(header: it) })
        } else {
            heads.eachWithIndex {
                Object text, int index ->
                    if(columns.size() > index) {
                        columns[index].footer = text
                    }
            }
        }
    }
    /**
     * Add a single row
     * @param cells
     */
    void addRow(Collection cells) {
        if(columns.empty) {
            columns.addAll(cells.collect { new Column() })
        }
        rows << new Row(cells : cells)
    }
    /**
     * Add multiple rows at once
     * @param rows
     */
    void setRows(Collection<Collection> rows) {
        rows.each {
            addRow(it)
        }
    }
    /**
     * Alias of addRow
     * @param cells
     */
    void leftShift(Collection cells) {
        addRow(cells)
    }
    /**
     * Add footer as text
     * @param foot
     */
    void setFooter(String foot) {
        if(! columns.empty) {
            setFooter([foot])
        } else {
            Log.w("Footer can not be set until data exists (current limitation)")
        }
    }
    /**
     * Add table footer
     * @param feet
     */
    void setFooter(Collection feet) {
        if(columns.empty) {
            columns.addAll(feet.collect { new Column(header: it, expandFooter: feet.size() == 1) })
        } else {
            feet.eachWithIndex {
                Object text, int index ->
                    if(columns.size() > index) {
                        columns[index].footer = text.toString()
                        columns[index].expandFooter = feet.size() == 1
                    }
            }
        }
    }
    /**
     * Generate table
     * @return
     */
    //@Override
    String toString(boolean compacted = compact) {
        List<String> lines = []
        Map<String, String> s = style.all
        // Add border color
        if(borderColor) {
            s.keySet().each {
                s[it] = borderColor + s[it] + RESET
            }
        }
        // Initialize map with min values
        Map<Column, Double> colWidthStats = columns.collectEntries {
            [(it) : it.minLen ]
        }
        // Initialize map with true
        Map<Column, Boolean> emptyCol = columns.collectEntries {
            [(it) : true]
        }
        columns.collect { it.header }.eachWithIndex {
            Object entry, int i ->
                int cellWidth = getDisplayWidth(decolor(entry.toString()))
                Column col = columns.get(i)
                col.length = [col.minLen, col.length, cellWidth].max()
                colWidthStats[col] = (colWidthStats[col] + cellWidth) / 2d
        }
        rows.each {
            it.cells.eachWithIndex {
                Object entry, int i ->
                    int cellWidth = getDisplayWidth(decolor(columns.get(i).format(entry, false)))
                    Column col = columns.get(i)
                    col.length = [col.minLen, col.length, cellWidth].max()
                    colWidthStats[col] = (colWidthStats[col] + cellWidth) / 2d
                    if(cellWidth > 0 ||! col.hideWhenEmpty) {
                        emptyCol[col] = false
                    }
            }
        }
        // Merge footer in several columns if its of length 1
        if (!columns.first().expandFooter) {
            columns.collect { it.footer }.eachWithIndex {
                Object entry, int i ->
                    int cellWidth = getDisplayWidth(decolor(entry.toString()))
                    Column col = columns.get(i)
                    col.length = [col.minLen, col.length, cellWidth].max()
                    colWidthStats[col] = (colWidthStats[col] + cellWidth) / 2d
            }
        }

        columns.each {
            Column col ->
                if(col.autoCollapse) {
                    col.maxLen = [col.minLen, Math.round(colWidthStats[col]) as int].max()
                }
                if(col.hideWhenEmpty && emptyCol.get(col)) {
                    col.length = 0
                    col.maxLen = 0
                    col.minLen = 0
                }
        }

        Closure getHR = {
            String sep, boolean border ->
                String ch = border ? s.hb : s.rs
                return ch + columns.collect {
                    ch * it.maxLen
                }.findAll { it != "" }.join(ch + sep + ch) + ch
        }

        // Top border
        if (style.window) {
            lines << (s.tl + getHR(s.hd, true) + s.tr)
        }
        if (hasHeaders()) {
            lines << ((style.window ? s.vb + " " : '') + columns
                .findAll { ! (it.hideWhenEmpty && emptyCol.get(it)) }
                .collect { it.fmtHeader }
                .join(" " + s.cs + " ") + (style.window ? " " + s.vb : ''))
            lines << (s.vr + getHR(s.in, false) + s.vl)
        }
        rows.each {
            Row row ->
                lines << ((style.window ? s.vb + " " : '') + row.cells.withIndex().collect {
                    Object cell, int i ->
                        Column col = columns.get(i)
                        boolean include = !(col.hideWhenEmpty && emptyCol.get(col))
                        return include ? col.format(cell) : null
                }.findAll { it != null }.join(" " + s.cs + " ") + (style.window ? " " + s.vb : ''))
                boolean lastRow = row == rows.last()
                if(lastRow &&! hasFooter()) {
                    lines << (s.bl + getHR(s.hu, true) + s.br)
                } else if(!compacted || lastRow) {
                    if(lastRow && columns.first().expandFooter) {
                        lines << (s.vr + getHR(s.hu, false) + s.vl)
                    } else {
                        lines << (s.vr + getHR(s.in, false) + s.vl)
                    }
                }
        }
        if (hasFooter()) {
            if(columns.first().expandFooter) {
                Column first = columns.first()
                lines << (
                    (style.window ? s.vb + " " : '') +
                    first.trimPad(first.footer.toString(), (columns.findAll { ! emptyCol.get(it) }.sum { it.maxLen } as int) + // We don't use fmtFooter as that one is trimmed
                    ((emptyCol.count { ! it.value } as int) - 1) * 3) + (style.window ? " " + s.vb : '')
                )
            } else {
                lines << (
                    (style.window ? s.vb + " " : '') +
                    columns
                        .findAll { ! (it.hideWhenEmpty && emptyCol.get(it)) }
                        .collect { it.fmtFooter }.join(" " + s.cs + " ") +
                    (style.window ? " " + s.vb : '')
                )
            }
            // Bottom border
            if (style.window) {
                lines << (s.bl + getHR(columns.first().expandFooter ? s.rs : s.hu, true) + s.br)
            }
        }
        return lines.join("\n")
    }
    /**
     * Return true if headers are set
     * @return
     */
    boolean hasHeaders() {
        return columns.any { it.header }
    }
    /**
     * Return true if footer is set
     * @return
     */
    boolean hasFooter() {
        return columns.any { it.footer }
    }
    /**
     * Prints table. Alternative you can pass a custom style to it
     * @param style
     */
    void print() {
        print(null, compact)
    }
    void print(boolean compacted, Stylable style = null) {
        print(style, compacted)
    }
    void print(Stylable style, boolean compacted = compact) {
        if(style) {
            this.style = style
        }
        println this.toString(compacted)
    }
    /**
     * Get display width of string
     * @param str
     * @return
     */
    static int getDisplayWidth(String str) {
        float i = str.length()
        str.toCharArray().each {
            int c = (it as char) as int
            // Only apply metrics when needed:
            if(c >= CharWidthMetrics.charStart) {
                float ratio = CharWidthMetrics.getRatio(it.toString())
                if(ratio > 0) {
                    i += (ratio - 1)
                }
            }
        }
        return Math.ceil(i) as int
    }
    /**
     * trim from the left (similar to substr but using display width)
     * @param str
     * @param ellipsis
     * @param ellipsisChar
     * @param maxLen
     * @return
     */
    static String trimDisplayLeft(String str, boolean ellipsis, String ellipsisChar, int maxLen) {
        String res = ""
        if(ellipsis) { maxLen-- }
        str.toCharArray().each {
            if(getDisplayWidth(res) < maxLen) {
                res += it
            }
        }
        return res + (ellipsis ? ellipsisChar : "")
    }

    /**
     * trim from the right (similar to substr but using display width)
     * @param str
     * @param ellipsis
     * @param ellipsisChar
     * @param maxLen
     * @return
     */
    static String trimDisplayRight(String str, boolean ellipsis, String ellipsisChar, int maxLen) {
        String res = ""
        if(ellipsis) { maxLen-- }
        str.toCharArray().toList().reverse().each {
            if(getDisplayWidth(res) < maxLen) {
                res = "" + it + res
            }
        }
        return (ellipsis ? ellipsisChar : "") + res
    }

    /**
     * trim from both sides using display width
     * @param str
     * @param ellipsis
     * @param ellipsisChar
     * @param maxLen
     * @return
     */
    static String trimDisplayBoth(String str, boolean ellipsis, String ellipsisChar, int maxLen) {
        List chars = str.toCharArray().toList()
        char ec = ellipsisChar[0] as char
        int left = 0
        int right = 0
        if(ellipsis) {
            chars[chars.size() - 1] = ec // Replace last char
            right++
        }
        do {
            chars.removeAt(chars.size() - right - 1) //Remove last (non-ellipsis) character
            if (getDisplayWidth(chars.join("")) > maxLen) {
                if(chars.first() != ec) {
                    chars[0] = ec //replace first with ellipsis
                    left++
                }
                chars.removeAt(left) // Remove first (non-ellipsis) character
            }
        } while(getDisplayWidth(chars.join("")) > maxLen)
        return chars.join("")
    }

    /**
     * Pad to the left using display width
     * @param str
     * @param len
     * @param pad
     * @return
     */
    static String padLeft(String str, int len, String pad = " ") {
        String res = str
        while(getDisplayWidth(res) < len) {
            res = pad + res
        }
        return res
    }

    /**
     * Pad to the right using display width
     * @param str
     * @param len
     * @param pad
     * @return
     */
    static String padRight(String str, int len, String pad = " ") {
        String res = str
        while(getDisplayWidth(res) < len) {
            res += pad
        }
        return res
    }
}
