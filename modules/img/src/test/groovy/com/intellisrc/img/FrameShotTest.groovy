package com.intellisrc.img

import spock.lang.Specification

import java.nio.file.Files

class FrameShotTest extends Specification {
    File imgTest = new File("src/test/resources/test.jpg")

    def "Convert File to FrameShot"() {
        setup:
            FrameShot fs = new FrameShot(imgTest)
        expect:
            assert fs : "File to FrameShot Conversion error..."
            assert fs.image.height > 0
            assert fs.image.width > 0
    }

    def "Save File"() {
        setup:
            boolean saved
            File out = Files.createTempFile("frame", "test.jpg").toFile()
            FrameShot fs = new FrameShot(imgTest)
        when:
            saved = fs.save(out)
        then:
            notThrown Exception
        expect:
            assert saved : "File not saved."
            assert out.exists()
            assert out.size()
        cleanup:
            if(out.exists()) {
                out.delete()
            }
    }

}
