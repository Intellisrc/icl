package com.intellisrc.cv.grab

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.cv.Converter
import com.intellisrc.cv.VideoGrab
import com.intellisrc.img.FrameShot
import groovy.transform.CompileStatic
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame

/**
 * Grab images from a video file
 *
 * FrameGrabber will try:
 "DC1394", "FlyCapture", "FlyCapture2", "OpenKinect", "PS3Eye", "VideoInput", "OpenCV", "FFmpeg", "IPCamera"
 *
 * @since 2020/02/19.
 */
@CompileStatic
class FrameVideoGrab extends VideoGrab {
    interface FrameFramer {
        boolean call(Frame img)
    }

    /**
     * Initialize Frame Grab with source
     * @param source
     */
    FrameVideoGrab(String source = "") {
        if(source) {
            this.source = source
        }
    }

    /**
     * Return a FrameShot instead of Frame
     * @param frameCallback
     * @param finishCallback
     */
    @Override
    void grabFrameShot(FrameShotCallback frameCallback, GrabFinished onFinish = null) {
        grab((FrameFramer) {
            Frame frame ->
                frameCallback.call(new FrameShot(Converter.FrameToBuffered(frame), "frame-" + SysClock.dateTime.format(timeFormat)))
        }, onFinish)
    }

    /**
     * Read all frames from source and call frameCallback for each of them
     * and finishCallback at the end
     * @param frameCallback
     * @param finishCallback
     */
    void grab(FrameFramer frameCallback, GrabFinished onFinish = null) {
        if(!isVideoFile(source)) {
            Log.w("Source doesn't seems to be a video file: %s", source)
        }
        Log.v("Reading encoded video: %s", source)
        boolean next = true
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(source)
        grabber.setFormat(format ?: source.split(/\./).last())
        grabber.start()

        while (next) {
            Frame frame = grabber.grab()
            if(frame) {
                if (frame.imageWidth) {
                    next = frameCallback.call(frame)
                }
            } else {
                next = false
            }
        }
        onFinish.call()
    }
}
