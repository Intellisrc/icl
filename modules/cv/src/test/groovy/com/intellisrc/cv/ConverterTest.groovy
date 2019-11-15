package com.intellisrc.cv

import org.bytedeco.opencv.opencv_core.IplImage
import spock.lang.Specification

import java.awt.image.BufferedImage

class ConverterTest extends Specification {
    File imgTest = new File("src/test/resources/test.jpg")
    
    def "Convert BufferedImage to IplImage"() {
        setup:
            assert imgTest.exists() : "Test file not found: " + imgTest.absolutePath
        when:
            BufferedImage img = Converter.FileToBuffered(imgTest)
            IplImage image = Converter.BufferedtoIplImage(img)
        then:
            notThrown Throwable
        expect:
            assert img : "File not found."
            assert image : "IplImage not converted."
            assert image.width() && image.height() : "Error in conversion"
            assert image.width() == img.width
    }

    def "Convert IplImage to BufferedImage"() {
        setup:
            IplImage image = Converter.FiletoIplImage(imgTest)
        expect:
            assert image : "IplImage not converted."
            assert image.width() && image.height() : "Error in conversion"
    }

    def "Convert Image to BufferedImage"() {
        //TODO
    }

    def "Convert Image to IplImage"() {
        //TODO

    }

    def "Convert Frame to IplImage"() {
        //TODO

    }

    def "Convert IplImage to Frame"() {
        //TODO

    }

    def "Convert Frame to BufferedImage"() {
        //TODO

    }

    def "Convert BufferedImage to Mat"() {
        setup:
            BufferedImage image = Converter.FileToBuffered(imgTest)
            assert image : "File not found."
        when:
            def width = Converter.BufferedToMat(image).arrayWidth()
            def height = Converter.BufferedToMat(image).arrayHeight()
        then:
            notThrown Throwable
        expect:
            assert width && height : "Error in conversion"
            assert width == image.width
            assert height == image.height
    }

}
