package com.intellisrc.img

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions

import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import java.awt.image.BufferedImage

/**
 * @since 18/11/20.
 */
@CompileStatic
class FileImgTools {
    static class Size {
        int width
        int height
    }
    /**
     * Return size of an image
     * @param image
     * @return
     */
    static Size getSize(File imageFile) {
        def image = ImageIO.read(imageFile)
        return new Size(width: image?.width, height: image?.height)
    }

    /**
     * Resize images without cropping
     * @param imgIn
     * @param imgOut
     * @param size
     * @return
     */
    static boolean resize(File imgIn, File imgOut, int size) {
        Thumbnails.of(imgIn).size(size, size).outputFormat('jpg').toFile(imgOut)
        return imgOut.exists()
    }
    /**
     * Resize images centering on the image and cropping extra parts
     * @param imgIn
     * @param imgOut
     * @param size
     * @return
     */
    static boolean resizeCentered(File imgIn, File imgOut, int size) {
        Thumbnails.of(imgIn).size(size, size).crop(Positions.CENTER).outputFormat('jpg').toFile(imgOut)
        return imgOut.exists()
    }
    /**
     * Resize images and crop without centering (keeping 0,0)
     * @param imgIn
     * @param imgOut
     * @param size
     * @return
     */
    static boolean resizeTopLeft(File imgIn, File imgOut, int size) {
        Thumbnails.of(imgIn).size(size, size).crop(Positions.TOP_LEFT).outputFormat('jpg').toFile(imgOut)
        return imgOut.exists()
    }
    /**
     * Resize images and crop based on Width
     * @param imgIn
     * @param imgOut
     * @param size
     * @return
     */
    static boolean resizeWidth(File imgIn, File imgOut, int size) {
        Thumbnails.of(imgIn).width(size).outputFormat('jpg').toFile(imgOut)
        return imgOut.exists()
    }
    /**
     * Resize images and crop based on Height
     * @param imgIn
     * @param imgOut
     * @param size
     * @return
     */
    static boolean resizeHeight(File imgIn, File imgOut, int size) {
        Thumbnails.of(imgIn).height(size).outputFormat('jpg').toFile(imgOut)
        return imgOut.exists()
    }

    /**
     * Rotate image from file and save to file
     * @param imgIn
     * @param imgOut
     * @param rotate
     * @return
     */
    static boolean rotate(File imgIn, File imgOut, int rotate) {
        if(imgOut.exists()) {
            imgOut.delete()
        }
        if(imgIn.exists()) {
            BufferedImage image = Converter.FileToBuffered(imgIn)
            BufferedImage rotated = BuffImgTools.rotate(image, rotate)
            Converter.BufferedToFile(rotated, imgOut)
        }
        return imgOut.exists()
    }

    /**
     * Crop an image and save it in a file
     * @param imgIn
     * @param imgOut
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    static boolean crop(File imgIn, File imgOut, int x, int y, int width, int height) {
        if(imgOut.exists()) {
            imgOut.delete()
        }
        if(imgIn.exists()) {
            BufferedImage image = Converter.FileToBuffered(imgIn)
            BufferedImage rotated = BuffImgTools.crop(image, x, y, width, height)
            Converter.BufferedToFile(rotated, imgOut)
        }
        return imgOut.exists()
    }

    /**
     * Checks if an image is a valid JPG
     * @param file
     * @return
     */
    static boolean isValidJPG(File file) {
        boolean valid = false
        //verify that it is not corrupted
        final InputStream digestInputStream = file.newInputStream()
        try {
            final ImageInputStream imageInputStream = ImageIO.createImageInputStream(digestInputStream)
            final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream)
            if (imageReaders.hasNext()) {
                final ImageReader imageReader = imageReaders.next()
                imageReader.setInput(imageInputStream)
                imageReader.read(0)?.flush()
                if (imageReader.formatName == "JPEG") {
                    imageInputStream.seek(imageInputStream.streamPosition - 2)
                    final byte[] lastTwoBytes = new byte[2]
                    imageInputStream.read(lastTwoBytes)
                    if (lastTwoBytes[0] == (byte) 0xff || lastTwoBytes[1] == (byte) 0xd9) {
                        valid = true
                    } else {
                        Log.w("File: ${file.name} is not complete.")
                    }
                }
            } else {
                try {
                    ImageIO.read(file).flush()
                    valid = true //ignore it
                } catch(Exception | Error ignored) {
                    Log.w("Simple image verification failed")
                }
            }
        } catch (Exception e) {
            Log.w("File: ${file.name} thrown an error: %s", e.message)
        } finally {
            digestInputStream.close()
        }
        return valid
    }
}
