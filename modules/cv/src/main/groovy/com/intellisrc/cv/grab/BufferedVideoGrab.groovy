package com.intellisrc.cv.grab

import com.intellisrc.core.Log
import com.intellisrc.core.SysClock
import com.intellisrc.cv.VideoGrab
import com.intellisrc.cv.jpg.MjpegFrame
import com.intellisrc.cv.jpg.MjpegInputStream
import com.intellisrc.img.FrameShot
import groovy.transform.CompileStatic

import java.awt.image.BufferedImage

/**
 * Read BufferedImages from a MJPEG video
 * @since 2020/02/19.
 */
@CompileStatic
class BufferedVideoGrab extends VideoGrab {

    interface BufferedFramer {
        boolean call(BufferedImage img)
    }

    BufferedVideoGrab(String source = "") {
        if(source) {
            this.source = source
        }
    }

    /**
     * Adds the protocol to a file if it doesn't have it
     */
    protected void addProtocolToSource() {
        if (!source.contains(':/')) {
            Log.v("Adding protocol to : %s", source)
            File srcFile = new File(source)
            if (srcFile.exists()) {
                source = srcFile.toURI().toURL()
            } else {
                Log.w("Not sure what to do with: %s, trying anyway...", source)
            }
        }
    }

    /**
     * Return a FrameShot instead of BufferedImage
     * @param frameCallback
     * @param finishCallback
     */
    @Override
    void grabFrameShot(FrameShotCallback frameCallback, GrabFinished onFinish = null) {
        grab((BufferedFramer) {
            BufferedImage image ->
                frameCallback.call(new FrameShot(image, "frame-" + SysClock.dateTime.format(timeFormat)))
        }, onFinish)
    }

    /**
     * Read all frames from mjpeg stream and call frameCallback for each of them
     * and finishCallback at the end
     * @param frameCallback
     * @param finishCallback
     */
    void grab(BufferedFramer frameCallback, GrabFinished onFinish = null) {
        addProtocolToSource()
        URL url = new URL(source)
        Log.v("Reading MJPG from: %s", url.toString())
        try {
            MjpegInputStream m = new MjpegInputStream(url.openStream())
            MjpegFrame frame
            boolean next = true
            while (next && (frame = m.readMjpegFrame()) != null) {
                if (frame) {
                    next = frameCallback.call(frame.image as BufferedImage)
                }
            }
        } catch(EOFException ignored) {
            Log.i("Video stream ended.")
            if(onFinish) {
                onFinish.call()
            }
        } catch (Exception e) {
            Log.e("Video playing failed.", e)
        }
    }
}
