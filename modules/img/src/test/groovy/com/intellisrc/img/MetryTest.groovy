package com.intellisrc.img

import spock.lang.Specification

import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D


/**
 * @since 2019/10/03.
 */
class MetryTest extends Specification {
    
    def "Coords to center and top left"() {
        setup:
            Rectangle2D rect = new Rectangle2D.Double(100,140,220,250)
        expect:
            // Only works if coordinates and size are even numbers, otherwise we may loose precision
            assert Metry.coordsToTopLeft(Metry.coordsToCenter(rect)).x == rect.x
            assert Metry.coordsToTopLeft(Metry.coordsToCenter(rect)).y == rect.y
            // These should never change:
            assert Metry.coordsToTopLeft(Metry.coordsToCenter(rect)).width == rect.width
            assert Metry.coordsToTopLeft(Metry.coordsToCenter(rect)).height == rect.height
    }
    
    def "Rotate rectangle"() {
        setup:
            Rectangle2D rect = new Rectangle2D.Double(190, 260, 320, 320)
            Rectangle2D newRect = Metry.rotateRect(Metry.coordsToCenter(rect), a, new Point2D.Double(75,75))
        expect:
            assert newRect.y == y
            assert newRect.x == x
        where:
            a | x   | y
            0 | 350 | 420
            20 | 451 | 305
            45 | 513 | 124
            80 | 463 | -136
            90 | 420 | -200
    }
}