package com.intellisrc.cv

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.bytedeco.javacpp.BytePointer

import static org.bytedeco.javacpp.helper.opencv_imgcodecs.cvLoadImage
import static org.bytedeco.javacpp.opencv_core.*
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite
import static org.bytedeco.javacpp.opencv_imgproc.*

/**
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
    static IplImage crop(final IplImage original, CvRect rect) {
        //TODO: sometimes rect seems to be incorrect. Check error:
        //Assertion failed: rect.width >= 0 && rect.height >= 0 && rect.x < image.width && rect.y < image.height && rect.x + rect.width >= (int)(rect.width > 0) && rect.y + rect.height >= (int)(rect.height > 0)
        cvSetImageROI(original, rect)
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
     * Converts a CvSeq to CvRect
     * @param seq
     * @return
     */
    static CvRect seqToRect(CvSeq seq, int index = 0) {
        BytePointer bp = cvGetSeqElem(seq, index)
        if(bp == null) {
            Log.e("Pointer was null")
        }
        CvPoint3D32f center = new CvPoint3D32f(bp)
        CvRect sRect = new CvRect(
            (center.x() - center.z()) as int,
            (center.y() - center.z()) as int,
            center.z() * 2 as int,
            center.z() * 2 as int
        )
        center.deallocate()
        return sRect
    }

    /**
     * Return a resized version of a CvRect increasing/decreasing its size by N pixels
     * @param CvRect
     * @param sizeAdjuster : Width and Height modifier
     * @return
     */
    static CvRect resizeRect(final CvRect rect, int sizeAdjuster) {
        return resizeRect(rect, sizeAdjuster, sizeAdjuster)
    }

    /**
     * Return a resized version of a CvRect in relation to its own size. eg. 1.5d will be 50% larger
     * @param rect
     * @param sizeAdjuster : pct to increase or reduce
     * @return
     */
    static CvRect resizeRect(final CvRect rect, double sizeAdjuster) {
        int width = Math.round(rect.width() * (sizeAdjuster - 1)) as int
        int height = Math.round(rect.height() * (sizeAdjuster - 1)) as int
        return resizeRect(rect, width, height)
    }
    /**
     * Return a resized version of a CvRect increasing/decreasing its size by N pixels
     * @param CvRect
     * @param sizeAdjusterW : Width in pixels to add to width
     * @param sizeAdjusterH : Height in pixels to add to height
     * @param maxWidth : maximum width limit
     * @param maxHeight : maximum height limit
     * @return
     */
    static CvRect resizeRect(final CvRect rect, int sizeAdjusterW, int sizeAdjusterH) {
        CvRect resRect = rect
        if(sizeAdjusterW) {
            int rectX = (rect.x() - (sizeAdjusterW * 0.5d)) as int
            int rectY = (rect.y() - (sizeAdjusterH * 0.5d)) as int
            int rectW = (rect.width() + sizeAdjusterW) as int
            int rectH = (rect.height() + sizeAdjusterH) as int
            resRect = new CvRect(rectX, rectY, rectW, rectH)
        }
        return resRect
    }

    /**
     * Translate coordinates from an scaled rect into the original image
     * @param original
     * @param resized
     * @param rect
     * @return
     */
    static CvRect scaleRect(CvSize original, CvSize resized, CvRect rect) {
        int newX = (rect.x() / resized.width() * original.width()) as int
        int newY = (rect.y() / resized.height() * original.height()) as int
        int newW = (rect.width() * (original.width() / resized.width())) as int
        int newH = (rect.height() * (original.height() / resized.height())) as int
        /*
        Log.v("ORIGINAL: %d x %d", original.width(), original.height())
        Log.v("RESIZED : %d x %d", resized.width(), resized.height())
        Log.v("RECT_ORG: %d x %d", rect.width(), rect.height())
        Log.v("          %d , %d", rect.x(), rect.y())
        Log.v("RECT_NEW: %d x %d", newW, newH)
        Log.v("          %d , %d", newX, newY)
        */
        return new CvRect(newX, newY, newW, newH)
    }
    /**
     * Translate coordinates to top-left
     * @param rect : with coordinates in center
     * @return
     */
    static CvRect coordsToTopLeft(CvRect rect) {
        double radiusX = rect.width() / 2d
        double radiusY = rect.height() / 2d
        int top  = Math.round(rect.x() - radiusX) as int
        int left = Math.round(rect.y() - radiusY) as int
        return new CvRect(top, left, rect.width(), rect.height())
    }
    /**
     * Translate coordinates to center
     * @param rect : with coordinates in top-left
     * @return
     */
    static CvRect coordsToCenter(CvRect rect) {
        double radiusX = rect.width() / 2d
        double radiusY = rect.height() / 2d
        int centerX = Math.round(rect.x() + radiusX) as int
        int centerY = Math.round(rect.y() + radiusY) as int
        return new CvRect(centerX, centerY, rect.width(), rect.height())
    }

    /**
     * Rotate a rectangle
     * @param rect : x,y its in center (use coordsToTopLeft if needed)
     * @param angle : clockwise in degrees (eg. 45)
     * @param pivot: if provided, will perform the rotation around that point (if not, around its own)
     *
     * https://stackoverflow.com/questions/4465931/rotate-rectangle-around-a-point
     * @return CvRect with coordinates in center
     */
    static CvRect rotateRect(CvRect rect, int angle, CvPoint pivot = null) {
        if(!pivot) {
            pivot = new CvPoint(rect.x() + Math.round(rect.width() / 2d) as int, rect.y() + Math.round(rect.height() / 2d) as int)
        }
        double rad = angle * Math.PI/180
        int width = rect.width()
        int height = rect.height()
        /*switch (true) {
            case angle > 45 &&  angle <= 135:
            case angle > 225 &&  angle <= 315:
                width = rect.height()
                height = rect.width()
                break
        }*/
        double diffX = rect.x() - pivot.x()
        double diffY = rect.y() - pivot.y()

        int x = Math.round(rect.x() - diffX + (diffX * Math.cos(rad)) + (diffY * Math.sin(rad))) as int
        int y = Math.round(rect.y() - diffY + (diffY * Math.cos(rad)) - (diffX * Math.sin(rad))) as int
        return new CvRect(x, y, width, height)
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
        } catch(Exception e) {}
    }
}
