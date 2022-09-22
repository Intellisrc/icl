package com.intellisrc.term.utils

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.SysInfo
import groovy.transform.CompileStatic

import java.awt.Canvas
import java.awt.Font
import java.awt.FontMetrics
/**
 * This class generates the dictionary to guess
 * character widths
 */
@CompileStatic
class CharWidthMetrics {
    static final File dictionary = Config.getFile("chars.widths.file", File.get("chars.widths.txt"))
    static final int charStart = 1304
    static final int charEnd = 984058

    static protected FontMetrics fm = null
    static protected int base = 0
    /**
     * Generates metrics file with the width calculations
     * @param dictFile
     */
    static void exportMetrics(File dictFile = dictionary) {
        if(dictFile == null) { dictFile = dictionary }
        Font font = new Font("Monospaced", Font.PLAIN, 12)
        dictFile.text = ""
        fm = new Canvas().getFontMetrics(font)
        Map<Float, List<String>> buffer = [:]
        base = fm.stringWidth("a")
        (charStart..charEnd).each { // Full is from 0..0x10ffff, but it can be reduced
        int cp ->
            String c = new StringBuilder().appendCodePoint(cp).toString()
            int w = fm.stringWidth(c)
            int diff = w - base
            if(diff != 0 && w > 0) {
                float ratio = (Math.round((w / base).toFloat() * 100) / 100f).toFloat()
                if(!buffer.containsKey(ratio)) { buffer[ratio] = [] }
                buffer[ratio] << Integer.toHexString(cp)
            }
        }
        buffer.keySet().sort().each {
            dictFile << (it + ":" + buffer[it].join(",") + SysInfo.newLine)
        }
    }
    /**
     * Import metrics file to get characters width
     * @param dictFile
     * @return
     */
    static Map<Integer, Float> importMetrics(File dictFile = dictionary) {
        if(dictFile == null) { dictFile = dictionary }
        if(! dictFile.exists()) {
            Log.w("Dictionary not found: %s, generating one...", dictFile.absolutePath)
            exportMetrics(dictFile)
        }
        Map<Integer, Float> metrics = [:]
        dictFile.eachLine {
            List<String> parts = it.tokenize(":")
            float ratio = parts.first().toFloat()
            parts.last().tokenize(",").each {
                metrics[Integer.parseInt(it, 16)] = ratio
            }
        }
        return metrics
    }
    /**
     * Calculate ratio of character
     * @param character
     * @return
     */
    static float getRatio(String character) {
        if(!fm) {
            Font font = new Font("Monospaced", Font.PLAIN, 12)
            fm = new Canvas().getFontMetrics(font)
            base = fm.stringWidth("a")
        }
        int w = fm.stringWidth(character)
        return (Math.round((w / base).toFloat() * 100) / 100f).toFloat()
    }
}
