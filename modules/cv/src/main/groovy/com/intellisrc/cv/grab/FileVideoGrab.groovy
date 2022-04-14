package com.intellisrc.cv.grab

import com.intellisrc.core.Config
import com.intellisrc.core.Log
import com.intellisrc.core.Millis
import com.intellisrc.cv.CvFrameShot
import com.intellisrc.cv.VideoGrab
import com.intellisrc.img.FrameShot
import groovy.transform.CompileStatic

/**
 * Read a directory and serve images in there
 * @since 2020/02/19.
 */
@CompileStatic
class FileVideoGrab extends VideoGrab {
    protected File directory
    protected String extension
    protected boolean running = false
    protected int fps

    interface FileFramer {
        boolean call(File img)
    }
    /**
     * Grab `extension` files from `directory`
     * @param directory
     * @param extension
     */
    FileVideoGrab(String source = "", File directory = null, String extension = "jpg", int fps = 24) {
        if(source) {
            this.source = source
        }
        this.directory = directory ?: new File(Config.get("video.dir", "/tmp"))
        this.extension = extension
        this.fps = fps
    }

    /**
     * Remove images from directory
     */
    void clearDir() {
        Log.i("Clearing images (*.%s) inside directory: %s", extension, directory.absolutePath)
        if (directory.exists()) {
            directory.eachFileMatchAsync("*."+extension) {
                it.delete()
            }
        }
    }

    /**
     * Keep pulling images from a directory
     * @param frameCallback
     * @param loopWait
     */
    void grabForever(FileFramer frameCallback, int loopWait = 500, GrabFinished onFinish = null) {
        Log.i("Grabbing forever (*.%s) images at : %s", extension, directory.absolutePath)
        running = true
        while(running) {
            grab(frameCallback)
            sleep(loopWait)
        }
        onFinish.call()
    }
    /**
     * Same as grabForever() but returning FrameShot instead of File
     * @param frameCallback
     * @param loopWait
     */
    void grabForeverFrameShot(FrameShotCallback frameCallback, int loopWait = 500, GrabFinished onFinish = null) {
        grabForever((FileFramer) {
            File file ->
                frameCallback.call(new FrameShot(file))
        }, loopWait, onFinish)
    }
    /**
     * Same as grabForever() but returning CvFrameShot instead of File
     * @param frameCallback
     * @param loopWait
     */
    void grabForeverCvFrameShot(CvFrameShotCallback frameCallback, int loopWait = 500, GrabFinished onFinish = null) {
        grabForever((FileFramer) {
            File file ->
                frameCallback.call(new CvFrameShot(file))
        }, loopWait, onFinish)
    }
    /**
     * Stop loop started by `grabForever`
     */
    void stopLoop() {
        running = false
    }

    /**
     * Returns a FrameShot instead of File
     * @param frameCallback
     * @param finishCallback
     */
    @Override
    void grabFrameShot(FrameShotCallback frameCallback, GrabFinished onFinish = null) {
        grab((FileFramer) {
            File file ->
                frameCallback.call(new FrameShot(file))
        }, onFinish)
    }

    /**
     * Read all images in directory and call frameCallback for each of them
     * and finishCallback at the end
     * @param frameCallback
     * @param finishCallback
     */
    void grab(FileFramer frameCallback, GrabFinished onFinish = null) {
        assert directory.exists() : "Directory: " + directory.absolutePath + " doesn't exists"
        Log.i("Grabbing (*.%s) images from : %s", extension, directory.absolutePath)
        List<File> files = directory.listFiles("*." + extension)
        if(!files.empty) {
            files.sort { a,b -> a.lastModified() <=> b.lastModified() }.any {
                File file ->
                    sleep(Math.round((Millis.SECOND / fps) as double).toInteger())
                    return ! frameCallback.call(file)
            }
        }
        onFinish.call()
    }
}
