package com.intellisrc.cv

import org.bytedeco.opencv.opencv_core.IplImage
import spock.lang.Specification

/**
 * @since 18/07/31.
 */
class CvToolsTest extends Specification {
    File imgTest = new File("src/test/resources/test.jpg")
    
    def "Rotate image"() {
        setup:
            IplImage img = CvTools.imgFromFile(imgTest)
            img = CvTools.resize(img, 600)
        expect:
            assert CvTools.rotate(img, 45).width() == 600
    }

}