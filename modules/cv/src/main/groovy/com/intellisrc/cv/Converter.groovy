package com.intellisrc.cv

import com.intellisrc.core.Log
import com.intellisrc.img.Converter as ImgConverter
import groovy.transform.CompileStatic
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.Java2DFrameUtils
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.opencv_core.IplImage
import org.bytedeco.opencv.opencv_core.Mat

import java.awt.image.BufferedImage

/**
 * Convert from and to different image formats, for example:
 * - BufferedImage (AWT)
 * - IplImage (OpenCV)
 * - Frame (JavaCV)
 * - Mat (OpenCV)
 *
 * @since 18/08/20.
 */
@CompileStatic
class Converter extends ImgConverter {
    ///////////////// TO BufferedImage ////////////////////
    /**
     * Convert Frame to BufferedImage
     * @param frame
     * @return
     */
    static BufferedImage FrameToBuffered(final Frame frame) {
        Java2DFrameConverter paintConverter = new Java2DFrameConverter()
        return paintConverter.getBufferedImage(frame, 1)
    }
    
    /**
     * Convert IplImage to BufferedImage
     * @param src
     * @return
     */
    static BufferedImage IplImageToBuffered(final IplImage src) {
        return FrameToBuffered(IplImageToFrame(src))
    }
    
    ///////////////// TO IplImage ////////////////////
    /**
     * Convert BufferedImage to IplImage
     * @param bufImage
     * @return
     */
    static IplImage BufferedtoIplImage(BufferedImage bufImage) {
        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage()
        Java2DFrameConverter java2dConverter = new Java2DFrameConverter()
        IplImage iplImage = iplConverter.convert(java2dConverter.convert(bufImage))
        return iplImage
    }
    
    /**
     * Convert Frame to IplImage
     * @param frame
     * @return
     */
    static IplImage FrameToIplImage(final Frame frame) {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage()
        return grabberConverter.convert(frame)
    }
    
    /**
     * Convert File to IplImage
     * @param file
     * @return
     */
    static IplImage FiletoIplImage(final File file) {
        IplImage img = null
        if(file.exists()) {
            img = CvTools.imgFromFile(file)
        } else {
            Log.w("Unable to find file: ${file.absolutePath}")
        }
        return img
    }
    
    ///////////////// TO Frame ////////////////////
    /**
     * Convert IplImage to Frame
     * @param src
     * @return
     */
    static Frame IplImageToFrame(final IplImage src) {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage()
        return grabberConverter.convert(src)
    }
    
    ///////////////// TO Mat ////////////////////
    /**
     * Convert buffered image to Mat
     */
    static Mat BufferedToMat(BufferedImage bi) {
        return Java2DFrameUtils.toMat(bi)
    }
}
