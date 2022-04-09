package com.intellisrc.term

import com.intellisrc.core.Log
import com.intellisrc.term.styles.SafeStyle
import com.intellisrc.term.styles.Stylable
import groovy.transform.CompileStatic
import static com.intellisrc.core.AnsiColor.removeColor
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
        String call(String cell)
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
        List<Cell> cells = []
    }
    /**
     * Information and format of a column
     */
    static class Column {
        int maxLen = 0
        /*boolean cut = false
        boolean ellipsis = false
        boolean wrap = false
        boolean show = true*/ //TODO
        boolean expandFooter = false // Single cell row
        Align align = LEFT

        String header = ""
        String footer = ""
        String getPaddedHeader() {
            return maxLen ? pad(header) : header
        }
        String getPaddedFooter() {
            return maxLen ? pad(footer) : footer
        }
        String pad(String text, int len = maxLen) {
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
    }
    /**
     * Cell text
     */
    static class Cell {
        Cell(String string, Formatter format = null) {
            text = string
            if(format) {
                formatter = format
            }
        }
        protected String text = ""
        Formatter formatter = { String it -> return it } as Formatter
        String getText() {
            return formatter.call(this.text)
        }
        String getTextNoColor() {
            return removeColor(getText())
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
    TableMaker(List<Map<String,String>> data, boolean footer) {
        setHeaders(data.first().keySet().toList())
        data.each {
            Map<String,String> entry ->
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
    TableMaker(List<List<String>> data, boolean headers, boolean footer) {
        if(headers) {
            setHeaders(data.pop())
        }
        data.each {
            List<String> entry ->
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
    void setHeaders(List<String> heads) {
        if(columns.empty) {
            columns.addAll(heads.collect { new Column(header: it) })
        } else {
            heads.eachWithIndex {
                String text, int index ->
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
    void addRow(List<String> cells) {
        if(columns.empty) {
            columns.addAll(cells.collect { new Column() })
        }
        rows << new Row(cells : cells.collect { new Cell(it) })
    }
    /**
     * Add row using cells
     * @param cells
     */
    void addCells(List<Cell> cells) {
        if(columns.empty) {
            columns.addAll(cells.collect { new Column() })
        }
        rows << new Row(cells : cells)
    }
    /**
     * Alias of addRow
     * @param cells
     */
    void leftShift(List<String> cells) {
        addRow(cells)
    }
    /**
     * Add footer as text
     * @param foot
     */
    void setFooter(String foot) {
        if(! columns.empty) {
            columns.first().footer = foot
        } else {
            Log.w("Footer can not be set until data exists (current limitation)")
        }
    }
    /**
     * Add table footer
     * @param feet
     */
    void setFooter(List<String> feet) {
        if(columns.empty) {
            columns.addAll(feet.collect { new Column(header: it, expandFooter: feet.size() == 1) })
        } else {
            feet.eachWithIndex {
                String text, int index ->
                    if(columns.size() > index) {
                        columns[index].footer = text
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
            String entry, int i ->
                columns.get(i).maxLen = [columns.get(i).maxLen, removeColor(entry).length()].max()
        }
        rows.each {
            it.cells.eachWithIndex {
                Cell entry, int i ->
                    columns.get(i).maxLen = [columns.get(i).maxLen, entry.textNoColor.length()].max()
            }
        }
        // Merge footer in several columns if its of length 1
        if (!columns.first().expandFooter) {
            columns.collect { it.footer }.eachWithIndex {
                String entry, int i ->
                    columns.get(i).maxLen = [columns.get(i).maxLen, removeColor(entry).length()].max()
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
            lines << ((style.window ? vb + " " : '') + columns.collect { it.paddedHeader }.join(" " + cs + " ") + (style.window ? " " + vb : ''))
            lines << (style.verticalRight + getHR(style.intercept, false) + style.verticalLeft)
        }
        rows.each {
            Row row ->
                lines << ((style.window ? vb + " " : '') + row.cells.withIndex().collect {
                    Cell cell, int col ->
                        columns.get(col).pad(cell.text)
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
                lines << ((style.window ? vb + " " : '') + columns.first().pad(columns.first().footer, (columns.sum { it.maxLen } as int) + (columns.size() - 1) * 3) + (style.window ? " " + vb : ''))
            } else {
                lines << ((style.window ? vb + " " : '') + columns.collect { it.paddedFooter }.join(" " + cs + " ") + (style.window ? " " + vb : ''))
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
