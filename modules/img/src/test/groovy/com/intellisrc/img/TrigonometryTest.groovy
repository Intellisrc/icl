package com.intellisrc.img


import spock.lang.Specification

import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D

/**
 * @since 19/06/26.
 * Recommended plotter: https://www.desmos.com/calculator
 */
class TrigonometryTest extends Specification {
    def "Get intersection points from circle. Horizontal"() {
        setup :
            Line2D line = new Line2D.Double(10, 27, -10, 27)
            Ellipse2D circle = new Ellipse2D.Double(-9, 11, 20, 20)
            Line2D intersection = Metry.intersect(line, circle)
        expect:
            assert intersection.x1 == -7
            assert intersection.y1 == 27
            assert intersection.x2 == 9
            assert intersection.y2 == 27
    }
    def "Get intersection points from circle. Vertical"() {
        setup :
            Line2D line = new Line2D.Double(10, 27, 10,-13)
            Ellipse2D circle = new Ellipse2D.Double(-9, 11, 20, 20)
            Line2D intersection = Metry.intersect(line, circle)
        expect:
            assert intersection.x1 == 10
            assert intersection.y1.round() == 25
            assert intersection.x2 == 10
            assert intersection.y2.round() == 17
    }
    def "Get intersection points from circle. Diagonal"() {
        setup :
            Line2D line = new Line2D.Double(10, 27, 1,10)
            Ellipse2D circle = new Ellipse2D.Double(-9, 11, 20, 20)
            Line2D intersection = Metry.intersect(line, circle)
        expect:
            assert intersection.x1.round() == 2
            assert intersection.y1.round() == 11
            assert intersection.x2.round() == 10
            assert intersection.y2.round() == 26
    }
    def "Get point of intersection of two lines"() {
        setup:
            Line2D line1 = new Line2D.Double(10, 250, 270, 130)
            Line2D line2 = new Line2D.Double(60, 120, 110,250)
        expect:
            Metry.intersect(line1, line2).with {
                assert x.round() == 95
                assert y.round() == 211
            }
    }
}