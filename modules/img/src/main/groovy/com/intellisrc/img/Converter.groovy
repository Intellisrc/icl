package com.intellisrc.img

import com.intellisrc.core.Log
import groovy.transform.CompileStatic

import javax.imageio.ImageIO
import java.awt.image.*

/**
 * Convert from and to different image formats, for example:
 * - BufferedImage (AWT)
 * - byte[], short[], int[]
 * - File
 *
 * @since 18/08/20.
 */
@CompileStatic
class Converter {
    ///////////////// TO BufferedImage ////////////////////
    /**
     * Convert File to BufferedImage
     * @param file
     * @return
     */
    static BufferedImage FileToBuffered(final File file) {
        BufferedImage img = null
        if(file.exists() && file.size() > 0) {
            try {
                img = ImageIO.read(file)
            } catch(IOException ignore) {
                Log.w("Image was broken: %s", file.absolutePath)
            }
        } else {
            Log.w("Unable to read file: %s", file.absolutePath)
        }
        return img
    }
    
    /**
     * Convert raw bytes to BufferImage
     * @param w
     * @param h
     * @param data
     * @param type : Another common type is: BufferedImage.TYPE_BYTE_GRAY
     * @return
     */
    //TODO: Untested
    static BufferedImage bytesToBuffered(int w, int h, byte[] data, int type = BufferedImage.TYPE_3BYTE_BGR) {
        DataBuffer buffer = new DataBufferByte(data, w*h)
        WritableRaster raster = Raster.createPackedRaster(buffer, w, h, 8, null)
        BufferedImage  image = new BufferedImage(w,h,type)
        image.setData(raster)
        return image
    }
    
    ///////////////// TO File ////////////////////
    /**
     * Saves BufferedImage into File
     * @param input
     * @param output
     */
    static void BufferedToFile(BufferedImage input, File output) {
        assert input : "Input image was null"
        ImageIO.write(input, "jpg", output)
    }
    
    ///////////////// TO byte[] ////////////////////
    /**
     * Convert BufferedImage to byte array
     * @param input
     * @return
     */
    //TODO: Untested
    static byte[] bufferedToBytes(BufferedImage input) {
        byte[] array = []
        //noinspection GroovyFallthrough
        switch ( input.type ) {
            case BufferedImage.TYPE_BYTE_GRAY :
            case BufferedImage.TYPE_3BYTE_BGR :
            case BufferedImage.TYPE_4BYTE_ABGR :
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_BYTE_BINARY :
            case BufferedImage.TYPE_BYTE_INDEXED :
                array = ((DataBufferByte) input.raster.dataBuffer).data
                break
            default:
                Log.w("Buffered Image is not byte type")
        }
        return array
    }
    /**
     * Convert BufferedImage into short array
     * @param input
     * @return
     */
    //TODO: Untested
    static short[] bufferedToShortArray(BufferedImage input) {
        short[] array = []
        //noinspection GroovyFallthrough
        switch ( input.type ) {
            case BufferedImage.TYPE_USHORT_GRAY :
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                array = ((DataBufferByte) input.raster.dataBuffer).data
                break
            default:
                Log.w("Buffered Image is not short array")
        }
        return array
    }
    /**
     * Convert BufferedImage into int array
     * @param input
     * @return
     */
    //TODO: Untested
    static int[] bufferedToIntArray(BufferedImage input) {
        int[] array = []
        //noinspection GroovyFallthrough
        switch ( input.type ) {
            case BufferedImage.TYPE_INT_RGB :
            case BufferedImage.TYPE_INT_BGR :
            case BufferedImage.TYPE_INT_ARGB :
            case BufferedImage.TYPE_INT_ARGB_PRE:
                array = ((DataBufferByte) input.raster.dataBuffer).data
                break
            default:
                Log.w("Buffered Image is not int array")
        }
        return array
    }
}
