package com.intellisrc.cv

import com.intellisrc.img.Metry
import groovy.transform.CompileStatic
import org.bytedeco.opencv.opencv_core.*

import java.awt.geom.Rectangle2D
import java.awt.geom.RectangularShape

import static org.bytedeco.opencv.global.opencv_core.cvGetSeqElem
import static org.bytedeco.opencv.global.opencv_core.cvPointFrom32f
import static org.bytedeco.opencv.global.opencv_highgui.*
import static org.bytedeco.opencv.global.opencv_imgproc.*

/**
 * Simple class to draw over an image.
 * It also can display images
 *
 * @since 18/07/30.
 */
@CompileStatic
class Drawing {
    String title = "View"
    IplImage image
    CvScalar color = CvScalar.BLACK
    int thickness = 2
    int delay = 0

    void circle(CvSeq seq, int sizeAdjuster = 0) {
        if(seq.total()) {
            CvPoint3D32f center = new CvPoint3D32f(cvGetSeqElem(seq, 0))
            CvPoint rCenter = cvPointFrom32f(new CvPoint2D32f(center.x(), center.y()))
            int radius = Math.round((center.z() + sizeAdjuster) as float)
            cvCircle(image, rCenter, radius, color, thickness, CV_AA, 0)
        }
    }

    void circle(RectangularShape rect, int sizeAdjuster = 0) {
        if(rect) {
            if(sizeAdjuster) {
                rect = Metry.resizeRect(rect, sizeAdjuster)
            }
            int radius = (rect.width / 2) as int
            CvPoint rCenter = cvPointFrom32f(new CvPoint2D32f(rect.x.toInteger() + radius, rect.y.toInteger() + radius))
            cvCircle(image, rCenter, radius, color, thickness, CV_AA, 0)
        }
    }

    void box(CvSeq seq, int sizeAdjuster = 0) {
        if(seq.total()) {
            box(CvTools.seqToRect(seq), sizeAdjuster)
        }
    }

    void box(final RectangularShape rect, int sizeAdjuster = 0) {
        if(rect) {
            Rectangle2D rectNew = rect as Rectangle2D
            if(sizeAdjuster) {
                rectNew = Metry.resizeRect(rect, sizeAdjuster)
            }
            cvRectangleR(image, CvTools.fromRectangle(rectNew), color, thickness, CV_AA, 0)
        }
    }

    void show(int msShow = delay) {
        cvShowImage(title,image)
        cvWaitKey(msShow)
    }

    void close() {
        cvDestroyWindow(title)
    }

    /**
     * Short cut to display just an image
     * @param img
     */
    static void show(IplImage img, String winTitle = "View", int msDelay = 0) {
        new Drawing(image: img, title: winTitle, delay: msDelay).show()
    }
}
