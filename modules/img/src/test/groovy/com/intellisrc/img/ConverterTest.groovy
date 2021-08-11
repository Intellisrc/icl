package com.intellisrc.img

import spock.lang.Specification

import java.awt.image.BufferedImage
import java.nio.file.Files

class ConverterTest extends Specification {
    File imgTest = new File("src/test/resources/test.jpg")

    def "Convert File to BufferedImage"() {
        setup:
            assert imgTest : "File not found."
        when:
            BufferedImage img = Converter.FileToBuffered(imgTest)
        then:
            notThrown Exception
        expect:
            assert img
            assert img.width && img.height : "Error in conversion"
    }

    def "Convert BufferedImage to File"() {
        setup:
            assert imgTest : "File not found."
            BufferedImage img = Converter.FileToBuffered(imgTest)
            File out = Files.createTempFile("buff", "test.jpg").toFile()
        when:
            Converter.BufferedToFile(img, out)
        then:
            notThrown Exception
        when:
            assert out.exists()
            assert out.size()
        then:
            BufferedImage imgOut = Converter.FileToBuffered(out)
        expect:
            assert imgOut.width == img.width
            assert imgOut.height == img.height
        cleanup:
            if(out.exists()) {
                out.delete()
            }
    }

}
