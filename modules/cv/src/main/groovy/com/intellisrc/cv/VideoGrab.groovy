package com.intellisrc.cv

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.img.FrameShot
import groovy.transform.CompileStatic

/**
 * Base class to grab images from a device, a path, etc.
 *
 * Other possible child classes may use:
 *
 //OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(deviceStr)
 //MJpegGrabber grabber = new MJpegGrabber(deviceStr)
 *
 * Alternative, you can use: http://webcam-capture.sarxos.pl/
 *
 * @since 18/08/20.
 */
@CompileStatic
abstract class VideoGrab {
    static {
        if(Config.exists("video.device")) {
            Log.w("[video.device] config key was renamed to [video.source]. Please fix it.")
        }
    }

    interface FrameShotCallback {
        boolean call(FrameShot frame)
    }
    interface CvFrameShotCallback {
        boolean call(CvFrameShot frame)
    }
    interface GrabFinished {
        void call()
    }

    protected String source = Config.get("video.source")
    protected String format = "" //auto
    protected String timeFormat = "yyMMdd-HHmmss.SSS"

    /**
     * Set source
     * @param source
     */
    void setSource(String source) {
        this.source = source
    }
    /**
     * Set video format
     * @param format
     */
    void setFormat(String format) {
        this.format = format
    }

    static boolean isVideoFile(String source) {
        return source =~ /(?i)\.(mpe?g|mp4|avi|mov|ogv|3gp|m4a)$/
    }

    /**
     *
     * @param source
     * @return
     */
    static boolean isMPEGVideoFile(String source) {
        return source =~ /(?i)\.(mjpe?g)$/
    }

    /**
     * Checks if a source is a stream
     * @param source
     * @return
     */
    static boolean isStream(String source) {
        boolean url = false
        try {
            //noinspection GroovyResultOfObjectAllocationIgnored
            new URI(source)
            url = true
        } catch(MalformedURLException ignore) {}
        return url
    }

    abstract void grabFrameShot(FrameShotCallback frameCallback, GrabFinished onFinish = null)

    /**
     * Return a CvFrameShot instead of FrameShot
     * @param frameCallback
     * @param finishCallback
     */
    void grabCvFrameShot(CvFrameShotCallback frameCallback, GrabFinished onFinish = null) {
        grabFrameShot({
            FrameShot shot ->
                frameCallback.call(new CvFrameShot(shot))
        }, onFinish)
    }
}
