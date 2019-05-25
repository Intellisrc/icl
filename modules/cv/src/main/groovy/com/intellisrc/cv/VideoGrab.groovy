package com.intellisrc.cv

import com.intellisrc.core.Log
import com.intellisrc.core.Config
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame

import java.awt.image.BufferedImage

import static org.bytedeco.javacpp.opencv_core.*
import groovy.transform.CompileStatic
/**
 * @since 18/08/20.
 */
@CompileStatic
class VideoGrab {
    static File exportDir = new File(Config.get("video.dir") ?: "/tmp")
    static String videoDevice = Config.get("video.device") ?: "/dev/video0"
    static String exportName = "live.jpg"
    private static int counter = 0

    static {
        if(!exportDir.exists()) {
            exportDir.mkdirs()
        }
    }

    interface Framer<T> {
        boolean call(T frame)
    }

    final Framer framerCallback

    VideoGrab(final Framer framer) {
        framerCallback = framer
    }

    static boolean isVideoFile(String filename) {
        return filename =~ /(?i)\.(mpe?g|mp4|avi|mov|ogv|3gp|m4a)$/
    }

    static boolean isMPEGVideoFile(String filename) {
        return filename =~ /(?i)\.(mjpe?g)$/
    }

    /**
     * Start getting video.
     * @param source can be a file, a URL or a keyword
     */
    void start(String source  = "") {
        boolean isFramerFile = framerCallback instanceof Framer<File>
        if(source.empty) {
            source = videoDevice
        }
        /** FrameGrabber will try:  (to try later)
            "DC1394", "FlyCapture", "FlyCapture2", "OpenKinect", "PS3Eye", "VideoInput", "OpenCV", "FFmpeg", "IPCamera"
            //OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceStr)
            //MJpegGrabber grabber = new MJpegGrabber(deviceStr)
            //OpenCVFrameConverter.ToIplImage toIplImage = new OpenCVFrameConverter.ToIplImage()
         */
        if(source == "camera") {
            File last = new File(exportDir, (++counter) + "-" + exportName)
            if(counter >= 1000) { counter = 0 }
            if(isFramerFile) {
                framerCallback.call(last)
            } else {
                framerCallback.call(Converter.FiletoIplImage(last))
            }
        } else if(isVideoFile(source)) {
            Log.v("Reading encoded video: %s", source)
            boolean next = true
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(source)
            grabber.setFormat(source.split(/\./).last())
            try {
                grabber.start()
                while (next) {
                    Frame frame = grabber.grab()
                    if(frame?.imageWidth) {
                        if(isFramerFile) {
                            File last = new File(exportDir, (++counter) + "-" + exportName)
                            if(counter >= 100) { counter = 0 }
                            Converter.ByteBufferToFile(Converter.FrameToBuffered(frame), last)
                            next = framerCallback.call(last)
                        } else {
                            IplImage img = Converter.FrameToIplImage(frame)
                            next = framerCallback.call(img)
                        }
                    } else {
                        next = false
                    }
                }
                Log.i("No more frames in video.")
            } catch (Exception e) {
                Log.e("Video playing failed.", e)
            }
        } else {
            if(!source.contains(':/')) {
                Log.v("Adding protocol to : %s", source)
                File srcFile = new File(source)
                if(srcFile.exists()) {
                    source = srcFile.toURI().toURL()
                } else {
                    Log.w("Not sure what to do with: %s, trying anyway...", source)
                }
            }
            URL url = new URL(source)
            Log.v("Reading MJPG from: %s", url.toString())
            try {
                MjpegInputStream m = new MjpegInputStream(url.openStream())
                MjpegFrame f
                boolean next = true
                while (next && (f = m.readMjpegFrame()) != null) {
                    if(isFramerFile) {
                        File last = new File(exportDir, (++counter) + "-" + exportName)
                        if(counter >= 100) { counter = 0 }
                        Converter.ByteBufferToFile((BufferedImage) f.getImage(), last)
                        next = framerCallback.call(last)
                    } else {
                        next = framerCallback.call(Converter.BufferedtoIplImage((BufferedImage) f.getImage()))

                    }
                }
                Log.i("Stream has stopped")
            } catch (Exception e) {
                Log.e("Video playing failed.", e)
            }
        }
    }
}
