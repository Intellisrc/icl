package com.intellisrc.cv.video

import com.intellisrc.core.Log
import com.intellisrc.core.Config
import com.intellisrc.core.SysClock
import com.intellisrc.cv.Converter
import com.intellisrc.cv.CvFrameShot
import com.intellisrc.img.FrameShot
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame

import java.awt.image.BufferedImage
import org.bytedeco.opencv.opencv_core.IplImage

import groovy.transform.CompileStatic

/**
 * @since 18/08/20.
 */
@CompileStatic
class VideoGrab {
    static File exportDir = new File(Config.get("video.dir") ?: "/tmp")
    static String videoDevice = Config.get("video.device") ?: "/dev/video0"
    static String exportName = ".jpg"
    static String timeFormat = "yyMMdd-HHmmss.SSS"

    static {
        if(!exportDir.exists()) {
            exportDir.mkdirs()
        } else { //Clear on start
            clearExportDir()
        }
    }
    
    static void clearExportDir() {
        if(exportDir.exists()) {
            exportDir.eachFile {
                it.delete()
            }
        }
    }
    
    static interface Framer<T> {
        boolean call(T frame)
    }
    
    static interface Finish {
        void call()
    }

    final Framer framerCallback
    final Finish onFinishCallback
    
    VideoGrab(final Framer framer, final Finish onFinish = null) {
        framerCallback = framer
        onFinishCallback = onFinish
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
            File last = new File(exportDir, SysClock.dateTime.format(timeFormat) + exportName)
            switch (true) {
                case framerCallback instanceof Framer<File> :
                    framerCallback.call(last)
                    break
                case framerCallback instanceof Framer<BufferedImage> :
                    framerCallback.call(Converter.FileToBuffered(last))
                    break
                case framerCallback instanceof Framer<IplImage> :
                    framerCallback.call(Converter.FiletoIplImage(last))
                    break
                case framerCallback instanceof Framer<Frame> :
                    //framerCallback.call(Converter.FiletoFrame(last))
                    Log.w("Framer format: <Frame> not implemented for camera source")
                    break
                case framerCallback instanceof Framer<FrameShot> :
                    framerCallback.call(new FrameShot(last))
                    break
                case framerCallback instanceof Framer<CvFrameShot> :
                    framerCallback.call(new CvFrameShot(last))
                    break
                default:
                    Log.w("Framer type unknown")
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
                        switch (true) {
                            case framerCallback instanceof Framer<File> :
                                File last = new File(exportDir, SysClock.dateTime.format(timeFormat) + exportName)
                                Converter.BufferedToFile(Converter.FrameToBuffered(frame), last)
                                next = framerCallback.call(last)
                                break
                            case framerCallback instanceof Framer<BufferedImage> :
                                next = framerCallback.call(Converter.FrameToBuffered(frame))
                                break
                            case framerCallback instanceof Framer<IplImage> :
                                IplImage img = Converter.FrameToIplImage(frame)
                                next = framerCallback.call(img)
                                break
                            case framerCallback instanceof Framer<Frame> :
                                next = framerCallback.call(frame)
                                break
                            case framerCallback instanceof Framer<FrameShot> :
                                next = framerCallback.call(new FrameShot(Converter.FrameToBuffered(frame),"frame-" + SysClock.dateTime.format(timeFormat)))
                                break
                            case framerCallback instanceof Framer<CvFrameShot> :
                                next = framerCallback.call(new CvFrameShot(Converter.FrameToBuffered(frame),"frame-" + SysClock.dateTime.format(timeFormat)))
                                break
                            default:
                                Log.w("Framer type unknown")
                        }
                    } else {
                        next = false
                    }
                }
                Log.i("No more frames in video.")
                if(onFinishCallback) {
                    onFinishCallback.call()
                }
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
                    if(f) {
                        BufferedImage image = (BufferedImage) f.getImage()
                        switch (true) {
                            case framerCallback instanceof Framer<File>:
                                File last = new File(exportDir, SysClock.dateTime.format(timeFormat) + exportName)
                                Converter.BufferedToFile(image, last)
                                next = framerCallback.call(last)
                                break
                            case framerCallback instanceof Framer<BufferedImage>:
                                next = framerCallback.call(image)
                                break
                            case framerCallback instanceof Framer<IplImage>:
                                next = framerCallback.call(Converter.BufferedtoIplImage(image))
                                break
                            case framerCallback instanceof Framer<Frame>:
                                //next = framerCallback.call(Converter.BufferedtoFrame(image)
                                Log.w("Framer format: <Frame> not implemented for mjpeg source")
                                break
                            case framerCallback instanceof Framer<FrameShot>:
                                next = framerCallback.call(new FrameShot(image, "frame-" + SysClock.dateTime.format(timeFormat)))
                                break
                            case framerCallback instanceof Framer<CvFrameShot>:
                                next = framerCallback.call(new CvFrameShot(image, "frame-" + SysClock.dateTime.format(timeFormat)))
                                break
                            default:
                                Log.w("Framer type unknown")
                        }
                    }
                }
                Log.i("Stream has stopped")
            } catch (Exception e) {
                Log.e("Video playing failed.", e)
            }
        }
    }
}
