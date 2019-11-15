package com.intellisrc.img

import groovy.transform.AutoClone
import groovy.transform.CompileStatic

import java.awt.image.BufferedImage
/**
 * Represents a single frame
 * @since 2019/08/01.
 */
@CompileStatic
@AutoClone
class FrameShot {
    private BufferedImage image
    @SuppressWarnings("GrFinalVariableAccess")
    final String name
    /**
     * General constructor
     * @param img
     * @param fileName
     */
    FrameShot(final BufferedImage img, String fileName = "none.jpg") {
        image = img
        name = fileName.replace(".tiff", ".jpg")
    }
    /**
     * Use this method to convert File to FrameShot
     * @param file
     * @return
     */
    FrameShot(final File file, boolean convertToColor = true) {
        assert file.exists()
        BufferedImage original = Converter.FileToBuffered(file)
        if(original) {
            if (convertToColor) {
                //Convert image into BGR
                BufferedImage rgbImg = new BufferedImage(original.width, original.height, BufferedImage.TYPE_3BYTE_BGR)
                rgbImg.graphics.drawImage(original, 0, 0, null)
                image = rgbImg
            } else {
                image = original
            }
        } else {
            image = null
        }
        name = file.name
    }
    /**
     * Return the image
     * @return
     */
    BufferedImage getImage() {
        return image
    }
    /**
     * Export FrameShot into a file
     * @param file
     * @return
     */
    boolean save(final File file) {
        Converter.BufferedToFile(image, file)
        return file.exists()
    }
    
    /**
     * Release image
     */
    void release() {
        if(image) {
            image.flush()
            image = null
        }
    }
}
