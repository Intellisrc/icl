package com.intellisrc.cv

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import static org.bytedeco.javacpp.opencv_core.IplImage

/**
 * @since 18/08/20.
 *
 */
@CompileStatic
class Converter {
    static IplImage BufferedtoIplImage(BufferedImage bufImage) {
        OpenCVFrameConverter.ToIplImage iplConverter = new OpenCVFrameConverter.ToIplImage()
        Java2DFrameConverter java2dConverter = new Java2DFrameConverter()
        IplImage iplImage = iplConverter.convert(java2dConverter.convert(bufImage))
        return iplImage
    }

    static IplImage FrameToIplImage(final Frame frame) {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage()
        return grabberConverter.convert(frame)
    }

    static Frame IplImageToFrame(final IplImage src) {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage()
        return grabberConverter.convert(src)
    }

    static BufferedImage FrameToBuffered(final Frame frame) {
        Java2DFrameConverter paintConverter = new Java2DFrameConverter()
        return paintConverter.getBufferedImage(frame, 1)
    }

    static BufferedImage IplImageToBuffered(final IplImage src) {
        return FrameToBuffered(IplImageToFrame(src))
    }

    static BufferedImage FileToBuffered(final File file) {
        return ImageIO.read(file)
    }

    static IplImage FiletoIplImage(final File file) {
        IplImage img = null
        if(file.exists()) {
            img = CvTools.imgFromFile(file)
        } else {
            Log.w("Unable to find file: ${file.absolutePath}")
        }
        return img
    }

    static void ByteBufferToFile(BufferedImage input, File output) {
        ImageIO.write(input, "jpg", output)
    }
}
