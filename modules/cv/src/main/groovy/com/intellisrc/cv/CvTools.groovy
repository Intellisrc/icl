package com.intellisrc.cv

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.opencv.opencv_core.*

import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RectangularShape

import static org.bytedeco.opencv.global.opencv_core.*
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite
import static org.bytedeco.opencv.global.opencv_imgproc.*
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvLoadImage

/**
 * Methods to work with IplImage objects (OpenCV, JavaCV)
 * @since 18/07/30.
 */
@CompileStatic
class CvTools {
    
    /**
     * Get Image from file
     * @param file
     * @return
     *
     * NOTE: --IMPORTANT--
     * All images initialized with cvLoadImage MUST be released using: cvReleaseImage()
     * If the image was created (not loaded), use .release()
     */
    static IplImage imgFromFile(final File file) {
        assert file.exists(): "Image not found: ${file.absolutePath}"
        // Load an image
        return cvLoadImage(file.path)
    }
    
    /**
     * Resize an image
     * @param src
     * @param width
     * @param height
     * @return
     */
    static IplImage resize(final IplImage src, int width, int height = 0) {
        if (!height) {
            CvSize size = cvGetSize(src)
            if(size.width()) {
                height = Math.round(size.height() * ((width / size.width()) as double)) as int
            } else {
                return src
            }
        }
        IplImage image = IplImage.create(width, height, src.depth(), src.nChannels())
        //resize the image
        cvResize(src, image)
        return image
    }
    
    /**
     * Rotate an image N degrees
     * @return
     */
    static IplImage rotate(final IplImage src, int angle) {
        int width = src.width()
        int height = src.height()
        switch (true) {
            case angle > 45 &&  angle <= 135:
            case angle > 225 &&  angle <= 315:
                width = src.height()
                height = src.width()
                break
        }
        IplImage image = IplImage.create(width, height, src.depth(), src.nChannels())
        CvPoint2D32f center = new CvPoint2D32f((src.width() * 0.5d) as float, (src.height() * 0.5d) as float)
        CvMat rotMat = cvCreateMat(2, 3, CV_32F)
        cv2DRotationMatrix(center, angle, 1, rotMat)
        cvWarpAffine(src, image, rotMat)
        rotMat.release()
        center.deallocate()
        return image
    }
    
    /**
     * Crop image using a rectangle
     * @param original
     * @param area
     * @return
     */
    static IplImage crop(final IplImage original, RectangularShape rect) {
        cvSetImageROI(original, fromRectangle(rect) as CvRect)
        IplImage cropped = cvCreateImage(cvGetSize(original), original.depth(), original.nChannels())
        cvCopy(original, cropped)
        return cropped
    }
    
    /**
     * Convert an image to gray
     * @param original
     * @return
     */
    static IplImage grayScale(final IplImage original) {
        IplImage grayImg = cvCreateImage(cvGetSize(original), IPL_DEPTH_8U, 1)
        cvCvtColor(original, grayImg, CV_BGR2GRAY)
        return grayImg
    }
    
    /**
     * Convert Image to Black and White
     * @param original
     * @return
     */
    static IplImage blackWhite(final IplImage original) {
        IplImage bwImg = cvCreateImage(cvGetSize(original), IPL_DEPTH_8U, 1)
        cvThreshold(grayScale(original), bwImg, 127, 255, CV_THRESH_BINARY)
        return bwImg
    }
    
    /**
     * Specify image contrast
     * @param original
     * @return
     */
    static IplImage contrast(final IplImage original) {
        IplImage conImg = cvCreateImage(cvGetSize(original), IPL_DEPTH_8U, 1)
        cvEqualizeHist(original, conImg)
        return conImg
    }
    
    /**
     * Converts a CvSeq to Rectangle2D
     * @param seq
     * @return
     */
    static Rectangle2D seqToRect(CvSeq seq, int index = 0) {
        BytePointer bp = cvGetSeqElem(seq, index)
        if(bp == null) {
            Log.e("Pointer was null")
        }
        CvPoint3D32f center = new CvPoint3D32f(bp)
        Rectangle2D sRect = new Rectangle2D.Double(
                center.x() - center.z(),
                center.y() - center.z(),
                center.z() * 2,
                center.z() * 2
        )
        center.deallocate()
        return sRect
    }
    
    /**
     * Translate coordinates from an scaled rect into the original image
     * @param original
     * @param resized
     * @param rect
     * @return
     */
    static Rectangle2D scaleRect(CvSize original, CvSize resized, Rectangle2D rect) {
        int newX = (rect.x / resized.width() * original.width()) as int
        int newY = (rect.y / resized.height() * original.height()) as int
        int newW = (rect.width * (original.width() / resized.width())) as int
        int newH = (rect.height * (original.height() / resized.height())) as int
        /*
        Log.v("ORIGINAL: %d x %d", original.width(), original.height())
        Log.v("RESIZED : %d x %d", resized.width(), resized.height())
        Log.v("RECT_ORG: %d x %d", rect.width(), rect.height())
        Log.v("          %d , %d", rect.x(), rect.y())
        Log.v("RECT_NEW: %d x %d", newW, newH)
        Log.v("          %d , %d", newX, newY)
        */
        return new Rectangle2D.Double(newX, newY, newW, newH)
    }
    /**
     * Convert Rectangle2D to CvRect
     * @param rect
     * @return
     */
    static CvRect fromRectangle(RectangularShape rect) {
        return new CvRect(
                rect.x.toInteger(),
                rect.y.toInteger(),
                rect.width.toInteger(),
                rect.height.toInteger()
        )
    }
    /**
     * Converts a CvRect to Rectangle2D
     * @param rect
     * @return
     */
    static Rectangle2D toRectangle(CvRect rect) {
        return new Rectangle2D.Double(
                rect.x(), rect.y(), rect.width(), rect.height()
        )
    }
    /**
     * Converts a CvRect to Ellipse2D
     * @param rect
     * @return
     */
    static Ellipse2D toEllipse(CvRect rect) {
        return new Ellipse2D.Double(
                rect.x(), rect.y(), rect.width(), rect.height()
        )
    }
    
    /**
     * Convert CvPoint to Point2D
     * @param point
     * @return
     */
    static Point2D toPoint(CvPoint point) {
        return new Point2D.Double(point.x(), point.y())
    }
    /**
     * Convert Point2D to CvPoint
     * @param point
     * @return
     */
    static CvPoint fromPoint2D(Point2D point) {
        return new CvPoint(point.x.toInteger(), point.y.toInteger())
    }
    
    static IplImage matchTemplate(IplImage src, IplImage template) {
        IplImage out = new IplImage()
        cvMatchTemplate(src, template, out, TM_SQDIFF)
        return out
    }
    
    /**
     * As cvSaveImage is broken, this way works.
     * references:
     * https://github.com/bytedeco/javacv/issues/998
     * https://github.com/bytedeco/javacv/issues/1023
     *
     * @param path
     * @param image
     */
    static boolean save(String path, IplImage image) {
        return imwrite(path, new Mat(image))
    }
    
    /**
     * Try to release it in all ways
     * @param img
     */
    static release(IplImage img) {
        try {
            img.release()
            cvReleaseImage(img)
        } catch(Exception | Error ignored) {}
    }
}
