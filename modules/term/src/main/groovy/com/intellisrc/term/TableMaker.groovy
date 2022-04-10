package com.intellisrc.term

import com.intellisrc.core.Log
import com.intellisrc.term.styles.SafeStyle
import com.intellisrc.term.styles.Stylable
import groovy.transform.CompileStatic

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
        List cells = []
    }
    /**
     * Information and format of a column
     */
    static class Column {
        int length = 0 // The calculated length of the column
        int maxLen = 0 // The maximum length allowed (user input)
        boolean ellipsis = false
        boolean expandFooter = false // Single cell row
        Align align = LEFT
        Formatter formatter = { Object it -> return decolor(it.toString()) } as Formatter
        Formatter color = { Object it -> return "" } as Formatter // Return AnsiColor.* to color cell
        Object header = ""
        Object footer = ""
        String headerColor = ""
        String footerColor = ""
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
                    case LEFT:   padded = padded.padRight(len); break
                    case RIGHT:  padded = padded.padLeft(len); break
                    case CENTER:
                        int half = Math.round(len / 2d) as int
                        padded = padded.padLeft(half).padRight(len - half)
                        break
                }
            }
            return padded
        }
        String trim(String text, boolean useEllipsis = ellipsis, int len = getMaxLen()) {
            String trimmed = text
            if(len && text.length() > len) {
                trimmed = text.substring(0, len - (useEllipsis ? 1 : 0)) + (useEllipsis ? "…" : "")
            }
            return trimmed
        }
        String trimPad(String text, int len = getMaxLen()) {
            return trim(pad(text, len), ellipsis, len)
        }
        String format(Object cell, boolean trim = true) {
            String color = color.call(cell)
            return color + (trim ? trimPad(formatter.call(cell)) : formatter.call(cell)) + (color ? RESET : "")
        }
    }

    // Style used to generate table
    Stylable style = new SafeStyle()
    final List<Column> columns = []
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
    TableMaker(List<Map> data, boolean footer) {
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
    void setHeaders(List heads) {
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
    void addRow(List cells) {
        if(columns.empty) {
            columns.addAll(cells.collect { new Column() })
        }
        rows << new Row(cells : cells)
    }
    /**
     * Add multiple rows at once
     * @param rows
     */
    void setRows(List<List> rows) {
        rows.each {
            addRow(it)
        }
    }
    /**
     * Alias of addRow
     * @param cells
     */
    void leftShift(List cells) {
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
    void setFooter(List feet) {
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
        String rs = style.rowSeparator
        String cs = style.colSeparator
        String hb = style.horizontalBorder
        String vb = style.verticalBorder

        columns.collect { it.header }.eachWithIndex {
            Object entry, int i ->
                columns.get(i).length = [columns.get(i).length, decolor(entry.toString()).length()].max()
        }
        rows.each {
            it.cells.eachWithIndex {
                Object entry, int i ->
                    columns.get(i).length = [columns.get(i).length, decolor(columns.get(i).format(entry, false)).length()].max()
            }
        }
        // Merge footer in several columns if its of length 1
        if (!columns.first().expandFooter) {
            columns.collect { it.footer }.eachWithIndex {
                Object entry, int i ->
                    columns.get(i).length = [columns.get(i).length, decolor(entry.toString()).length()].max()
            }
        }

        Closure getHR = {
            String sep, boolean border ->
                String ch = border ? hb : rs
                return ch + columns.collect {
                    ch * it.maxLen
                }.join(ch + sep + ch) + ch
        }

        // Top border
        if (style.window) {
            lines << (style.topLeft + getHR(style.horizontalDown, true) + style.topRight)
        }
        if (hasHeaders()) {
            lines << ((style.window ? vb + " " : '') + columns.collect { it.fmtHeader }.join(" " + cs + " ") + (style.window ? " " + vb : ''))
            lines << (style.verticalRight + getHR(style.intercept, false) + style.verticalLeft)
        }
        rows.each {
            Row row ->
                lines << ((style.window ? vb + " " : '') + row.cells.withIndex().collect {
                    Object cell, int col ->
                        columns.get(col).format(cell)
                }.join(" " + cs + " ") + (style.window ? " " + vb : ''))
                boolean lastRow = row == rows.last()
                if(lastRow &&! hasFooter()) {
                    lines << (style.bottomLeft + getHR(style.horizontalUp, true) + style.bottomRight)
                } else if(!compacted || lastRow) {
                    if(lastRow && columns.first().expandFooter) {
                        lines << (style.verticalRight + getHR(style.horizontalUp, false) + style.verticalLeft)
                    } else {
                        lines << (style.verticalRight + getHR(style.intercept, false) + style.verticalLeft)
                    }
                }
        }
        if (hasFooter()) {
            if(columns.first().expandFooter) {
                Column first = columns.first()
                lines << (
                    (style.window ? vb + " " : '') +
                    first.trimPad(first.footer.toString(), (columns.sum { it.maxLen } as int) + // We don't use fmtFooter as that one is trimmed
                    (columns.size() - 1) * 3) + (style.window ? " " + vb : '')
                )
            } else {
                lines << (
                    (style.window ? vb + " " : '') +
                    columns.collect { it.fmtFooter }.join(" " + cs + " ") +
                    (style.window ? " " + vb : '')
                )
            }
            // Bottom border
            if (style.window) {
                lines << (style.bottomLeft + getHR(columns.first().expandFooter ? rs : style.horizontalUp, true) + style.bottomRight)
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
}
