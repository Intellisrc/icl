package com.intellisrc.term

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
        boolean cut = false
        boolean ellipsis = false
        boolean wrap = false
        boolean show = true
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
        Cell(String string) {
            text = string
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

    /**
     * Table style
     */
    static class Style {
        String topLeft   = '┌'
        String botLeft   = '└'
        String topRight  = '┐'
        String botRight  = '┘'
        String intercept = '┼'
        String vertRight = '├'
        String vertLeft  = '┤'
        String horzDown  = '┬'
        String horzUp    = '┴'
        String rowSeparator = '─'
        String colSeparator = '│'
        // Draw box around table
        boolean window = true
        // Draw borders
        boolean borders = true
        // If false, its simple characters
        boolean boxStyle = true
    }

    // Style used to generate table
    Style style = new Style()
    final List<Column> columns = []
    final List<Row> rows = []
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
     * Alias of addRow
     * @param cells
     */
    void leftShift(List<String> cells) {
        addRow(cells)
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
    @Override
    String toString() {
        List<String> lines = []
        String rs = style.rowSeparator.toString()
        String cs = style.colSeparator.toString()

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
            String sep ->
                return rs + columns.collect {
                    rs * it.maxLen
                }.join(rs + sep + rs) + rs
        }

        // Top border
        if (style.window) {
            lines << (style.topLeft + getHR(style.horzDown) + style.topRight)
        }
        if (hasHeaders()) {
            lines << ((style.window ? cs + " " : '') + columns.collect { it.paddedHeader }.join(" " + cs + " ") + (style.window ? " " + cs : ''))
            lines << (style.vertRight + getHR(style.intercept) + style.vertLeft)
        }
        rows.each {
            Row row ->
                lines << ((style.window ? cs + " " : '') + row.cells.withIndex().collect {
                    Cell cell, int col ->
                        columns.get(col).pad(cell.text)
                }.join(" " + cs + " ") + (style.window ? " " + cs : ''))
                boolean lastRow = row == rows.last() &&! hasFooter()
                if(lastRow) {
                    lines << (style.botLeft + getHR(style.horzUp) + style.botRight)
                } else {
                    if(columns.first().expandFooter) {
                        lines << (style.vertRight + getHR(style.horzUp) + style.vertLeft)
                    } else {
                        lines << (style.vertRight + getHR(style.intercept) + style.vertLeft)
                    }
                }
        }
        if (hasFooter()) {
            if(columns.first().expandFooter) {
                lines << ((style.window ? cs + " " : '') + columns.first().pad(columns.first().footer, (columns.sum { it.maxLen } as int) + (columns.size() - 1) * 3) + (style.window ? " " + cs : ''))
            } else {
                lines << ((style.window ? cs + " " : '') + columns.collect { it.paddedFooter }.join(" " + cs + " ") + (style.window ? " " + cs : ''))
            }
            // Bottom border
            if (style.window) {
                lines << (style.botLeft + getHR(columns.first().expandFooter ? rs : style.horzUp) + style.botRight)
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
    void print(Style style = null) {
        if(style) {
            this.style = style
        }
        println this.toString()
    }
}
