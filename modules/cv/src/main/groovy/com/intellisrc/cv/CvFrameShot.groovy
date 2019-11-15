package com.intellisrc.cv

import com.intellisrc.img.FrameShot
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import org.bytedeco.opencv.opencv_core.IplImage

import java.awt.image.BufferedImage
/**
 * Represents a single frame
 * This class extends FrameShot in `img` package to provide OpenCV support
 * @since 2019/08/01.
 */
@CompileStatic
@AutoClone
class CvFrameShot extends FrameShot {
    /**
     * General constructor
     * @param img
     * @param fileName
     */
    CvFrameShot(final BufferedImage img, String fileName = "none.jpg") {
        super(img, fileName)
    }
    /**
     * Use this method to convert IplImage to FrameShot
     * @param image
     * @param name
     * @return
     */
    CvFrameShot(final IplImage image, String name = "ipl-image.jpg") {
        super(Converter.IplImageToBuffered(image), name)
    }
    /**
     * Use this method to convert File to FrameShot
     * @param file
     * @return
     */
    CvFrameShot(final File file, boolean convertToColor = true) {
        super(file, convertToColor)
    }
    
    /**
     * Convert FrameShot into CvFrameShot
     * @param parent
     */
    CvFrameShot(final FrameShot parent) {
        super(parent.image, parent.name)
    }
    
    /**
     * Export FrameShot to IplImage
     * @return
     */
    IplImage getIplImage() {
        return Converter.BufferedtoIplImage(image)
    }
}
