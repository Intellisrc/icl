package com.intellisrc.img

import com.intellisrc.core.Log
import groovy.transform.CompileStatic
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.geometry.Positions

import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.awt.image.RasterFormatException

/**
 * Methods to perform operations on BufferedImage objects
 * @since 19/06/24.
 */
@CompileStatic
class BuffImgTools {
    static class Size {
        int width
        int height
    }
    /**
     * Return size from a Buffered Image
     * @param image
     * @return
     */
    static Size getSize(BufferedImage image){
        assert image : "Image was empty"
        return new Size(width: image?.width, height: image?.height)
    }
    /**
     * Resize BufferedImage
     * @param image
     * @param size
     * @return
     */
    static BufferedImage resize(BufferedImage image, int size) {
        assert image : "Image was empty"
        assert size : "Size was zero"
        return Thumbnails.of(image).size(size, size).asBufferedImage()
    }
    /**
     * Resize BufferedImage centering on the image and cropping extra parts
     * @param image
     * @param size
     * @return
     */
    static BufferedImage resizeCentered(BufferedImage image, int size) {
        assert image: "Image was empty"
        assert size : "Size was zero"
        return Thumbnails.of(image).size(size, size).crop(Positions.CENTER).asBufferedImage()
    }
    /**
     * Resize BufferedImage without centering (keeping 0,0)
     * @param image
     * @param size
     * @return
     */
    static BufferedImage resizeTopLeft(BufferedImage image, int size) {
        assert image: "Image was empty"
        assert size : "Size was zero"
        return Thumbnails.of(image).size(size, size).crop(Positions.TOP_LEFT).asBufferedImage()
    }
    /**
     * Resize BufferedImage based on Width
     * @param image
     * @param size
     * @return
     */
    static BufferedImage resizeWidth(BufferedImage image, int size) {
        assert image: "Image was empty"
        assert size : "Size was zero"
        return Thumbnails.of(image).width(size).asBufferedImage()
    }
    /**
     * Resize BufferedImage based on Height
     * @param image
     * @param size
     * @return
     */
    static BufferedImage resizeHeight(BufferedImage image, int size) {
        assert image: "Image was empty"
        assert size : "Size was zero"
        return Thumbnails.of(image).width(size).asBufferedImage()
    }
    /**
     * Rotate a BufferedImage
     * This method won't enlarge the dimensions of the image
     * FIXME: it doesn't work for angles rather than 90, 180, 270
     * @param image
     * @param angle
     * @return
     */
    static BufferedImage rotate(BufferedImage image, int angle) {
        assert image: "Image was empty"
        assert [0,90,180,270].contains(angle) : "Angle $angle is not supported yet"
        boolean flip = (angle > 45 && angle < 135) || (angle > 225 && angle < 315)
        int w = image.width
        int h = image.height
        int fw = flip ? h : w
        int fh = flip ? w : h
        BufferedImage rotated = new BufferedImage(fw, fh, image.getType())
        Graphics2D graphic = rotated.createGraphics()
        if(angle > 45 && angle < 135) {
            graphic.translate((h - w) / 2d, (h - w) / 2d)
            graphic.rotate(Math.toRadians(angle), (h / 2d), (w / 2d))
        } else if(angle > 225 && angle < 315) {
            graphic.translate((w - h) / 2d, (w - h) / 2d)
            graphic.rotate(Math.toRadians(angle), (h / 2d), (w / 2d))
        } else {
            graphic.rotate(Math.toRadians(angle), (w / 2d), (h / 2d))
        }
        graphic.drawImage(image, null, 0, 0)
        graphic.dispose()
        return rotated
    }

    /**
     * Crop a BufferedImage
     * @param image
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    static BufferedImage crop(final BufferedImage image, int x, int y, int width, int height) {
        assert image: "Image was empty"
        assert width > 0 : "No width specified"
        assert height > 0 : "No height specified"
        assert x + width <= image.width : "Crop is out of width"
        assert y + height <= image.height : "Crop is out of height"
        BufferedImage cropped = null
        try {
            cropped = image.getSubimage(x, y, width, height)
        } catch(RasterFormatException e) {
            Log.w("Cropping Image (%d x %d) to: [ %d x %d | x:%d, y:%d ] failed. %s", image.width, image.height, width, height, x, y, e)
        }
        return cropped
    }

    /**
     * Create a copy of a bufferedImage
     * Based in: https://stackoverflow.com/questions/3514158/
     * @param image
     * @return
     */
    static BufferedImage copy(BufferedImage source) {
        assert source: "Image was empty"

        BufferedImage b = new BufferedImage(source.width, source.height, source.type)
        Graphics g = b.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return b
    }
    
    /**
     * Class to paint a BufferedImage in a Frame
     */
    static class ImageComponent extends Component {
        private final BufferedImage img
    
        void paint(Graphics g) {
            g.drawImage(img, 0, 0, null)
        }
    
        ImageComponent(final BufferedImage img) {
            this.img = img
        }
    
        Dimension getPreferredSize() {
            return new Dimension(img.getWidth(), img.getHeight())
        }
    }
    /**
     * Display a Buffered image (usually for debugging purposes)
     * @param image
     */
    static void show(final BufferedImage image) {
        assert image: "Image was empty"
        Frame frame = new Frame()
        frame.addWindowListener(new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent e) {
                frame.dispose()
            }
        })
        frame.add(new ImageComponent(image))
        frame.setSize(image.width, image.height)
        Dimension dimension = Toolkit.defaultToolkit.screenSize
        int x = (int) ((dimension.width - frame.width) / 2)
        int y = (int) ((dimension.height - frame.height) / 2)
        frame.setLocation(x, y)
        frame.setVisible(true)
    }
}
