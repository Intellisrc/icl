package com.intellisrc.img

import spock.lang.Specification

import java.awt.image.BufferedImage

class BuffImgToolsTest extends Specification{
    File imgTest = new File("src/test/resources/test.jpg")

    def "Get Size"() {
        setup:
            BufferedImage img = new BufferedImage(640,840,BufferedImage.TYPE_3BYTE_BGR)
            BuffImgTools.Size imgSize = BuffImgTools.getSize(img)
        expect:
            assert imgSize.height > 0
            assert imgSize.width > 0
    }

    def "Resize"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.resize(img, 420)
        expect:
            assert img.width == 420
            assert img.height == 420
    }

    def "Resize Centered"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.resizeCentered(img, 320)
        expect:
            assert img.width == 320
            assert img.height == 320
    }

    def "Resize without centering"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.resizeTopLeft(img, 512)
        expect:
            assert img.width == 512
            assert img.height == 512
    }

    def "Resize width"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.resizeWidth(img, 840)
        expect:
            assert img.width == 840
            assert img.height == 840
    }

    def "Resize height"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.resizeHeight(img, 40)
        expect:
                assert img.width == 40
                assert img.height == 40
    }

    def "Rotate"() {
        setup:
            BufferedImage img = Converter.FileToBuffered(imgTest)
            //BuffImgTools.show(img)
            BufferedImage rotated = BuffImgTools.rotate(img, 90)
            //BuffImgTools.show(rotated)
        expect:
            assert rotated.width == img.height
            assert rotated.height == img.width
    }

    def "Crop BufferedImage"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.crop(img, 9, 90, 100, 100)
            println(img.width)
            println(img.height)
        expect:
            assert img.width != 640
            assert img.height != 640
    }
    def "Crop BufferedImage full size"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.crop(img, 0, 0, 640, 640)
        expect:
            assert img.width == 640
            assert img.height == 640
    }
    def "Crop BufferedImage on edge"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            img = BuffImgTools.crop(img, 540, 540, 100, 100)
            println(img.width)
            println(img.height)
        expect:
            assert img.width == 100
            assert img.height == 100
    }
    def "Crop BufferedImage outside of edge"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
        when:
            BuffImgTools.crop(img, 200, 200, 500, 500)
        then:
            thrown AssertionError
    }
    def "Crop BufferedImage no width / height"() {
        setup:
            BufferedImage img = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
        when:
            BuffImgTools.crop(img, 200, 200, 0, 0)
        then:
            thrown AssertionError
    }

    def "Create a copy of BufferedImage"() {
        setup:
            BufferedImage source = new BufferedImage(640,640,BufferedImage.TYPE_3BYTE_BGR)
            BufferedImage imgCopy = BuffImgTools.copy(source)
        expect:
            assert imgCopy.width == 640
            assert imgCopy.height == 640
    }



}
