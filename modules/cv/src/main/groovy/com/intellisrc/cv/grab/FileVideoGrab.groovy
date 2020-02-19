package com.intellisrc.cv.grab

import com.intellisrc.core.Config
import com.intellisrc.core.Log
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
    FileVideoGrab(File directory = new File(Config.get("video.dir") ?: "/tmp"),
                  String extension = "jpg", int fps = 24) {

        this.directory = directory
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
    void grabForever(FileFramer frameCallback, int loopWait = 500) {
        Log.i("Grabbing forever (*.%s) images at : %s", extension, directory.absolutePath)
        running = true
        while(running) {
            grab(frameCallback)
            sleep(loopWait)
        }
    }
    /**
     * Same as grabForever() but returning FrameShot instead of File
     * @param frameCallback
     * @param loopWait
     */
    void grabForeverFrameShot(FrameShotCallback frameCallback, int loopWait = 500) {
        grabForever((FileFramer) {
            File file ->
                frameCallback.call(new FrameShot(file))
        }, loopWait)
    }
    /**
     * Same as grabForever() but returning CvFrameShot instead of File
     * @param frameCallback
     * @param loopWait
     */
    void grabForeverCvFrameShot(CvFrameShotCallback frameCallback, int loopWait = 500) {
        grabForever((FileFramer) {
            File file ->
                frameCallback.call(new CvFrameShot(file))
        }, loopWait)
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
    void grabFrameShot(FrameShotCallback frameCallback) {
        grab((FileFramer) {
            File file ->
                frameCallback.call(new FrameShot(file))
        })
    }

    /**
     * Read all images in directory and call frameCallback for each of them
     * and finishCallback at the end
     * @param frameCallback
     * @param finishCallback
     */
    void grab(FileFramer frameCallback) {
        assert directory.exists() : "Directory: " + directory.absolutePath + " doesn't exists"
        Log.i("Grabbing (*.%s) images from : %s", extension, directory.absolutePath)
        List<File> files = directory.listFiles("*." + extension)
        if(!files.empty) {
            files.sort { a,b -> a.lastModified() <=> b.lastModified() }.any {
                File file ->
                    sleep(Math.round(1000d / fps).toInteger())
                    return ! frameCallback.call(file)
            }
        }
    }
}
