package com.intellisrc.cv

import org.bytedeco.opencv.opencv_core.IplImage
import spock.lang.Specification

class CvFrameShotTest extends Specification {
    File imgTest = new File("src/test/resources/test.jpg")

    def "Convert IplImage to FrameShot"() {
        setup:
            IplImage ipl = Converter.FiletoIplImage(imgTest)
            CvFrameShot fs = new CvFrameShot(ipl, "ipl-image.jpg")
        expect:
            assert fs : "IplImage to FrameShot Conversion error..."
            assert fs.image.height == ipl.height()
            assert fs.image.width == ipl.width()
    }

    def "Convert FrameShot to IplImage"() {
        setup:
            CvFrameShot fs = new CvFrameShot(imgTest)
            IplImage ipl = fs.iplImage
        expect:
            assert ipl : "FrameShot to IplImage conversion failed."
            assert fs.image.height == ipl.height()
            assert fs.image.width == ipl.width()
    }

}
