package com.intellisrc.img

import groovy.transform.CompileStatic

import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RectangularShape

/**
 * Class to perform common trigonometry calculations
 * @author : A.Lepe
 * @since 19/06/26.
 *
 * Implemented following the equations at:
 * http://www.ambrsoft.com/TrigoCalc/menu/menuTrigoCalc.htm
 */
@CompileStatic
class Metry {
    /**
     * Translate coordinates from one dimension to another:
     * It uses an "original size" to translate coordinates into a "new size"
     * @return
     */
    static Rectangle2D translateCoords(int origSize, int newSize, Rectangle2D origBox) {
        //Log.v("From size: %d, To size: %d (%f, %f, %f, %f)", origSize, newSize, origBox.x, origBox.y, origBox.width, origBox.height)
        double sx = origBox.x / origSize
        double sy = origBox.y / origSize
        double sw = origBox.width / origSize
        double sh = origBox.height / origSize
        return new Rectangle2D.Double(
                sx * newSize,
                sy * newSize,
                sw * newSize,
                sh * newSize
        )
    }
    /**
     * Translate coordinates from one dimension to another:
     * It uses an "original size" to translate coordinates into a "new size"
     * @return
     */
    static Point2D translateCoords(int origSize, int newSize, Point2D origPoint) {
        double sx = origPoint.x / origSize
        double sy = origPoint.y / origSize
        return new Point2D.Double(
                sx * newSize,
                sy * newSize
        )
    }
    /**
     * Will return a line with the two points which intersects with a given circle
     * @param line
     * @param circle : It can be Ellipse2D, Rectangle2D
     * @return
     */
    static Line2D intersect(Line2D line, RectangularShape circle) {
        Line2D intersecLine
        double m = getLineSlope(line)
        double d = getYcrossValue(line)
        double r = circle.width / 2d
        Point2D center = new Point2D.Double(
                (circle.x + (circle.width / 2d)).round().toInteger(),
                (circle.y + (circle.height / 2d)).round().toInteger()
        )
        double a = center.x
        double b = center.y
        if(m.isInfinite()) {
            double x = line.x1
            intersecLine = new Line2D.Double(
                    x,
                    Math.sqrt((r**2 - (x - a)**2).toDouble()) + b,
                    x,
                    -Math.sqrt((r**2 - (x - a)**2).toDouble()) + b
            )
        } else {
            double t = r**2 * (1 + m**2) - (b - (m * a) - d)**2
            /*
              If   t > 0	then two intersection points exists
              If   t = 0	then the line is tangent to the circle
              If   t < 0	then the line do not intersects the circle
             */
            intersecLine = new Line2D.Double(
                    (a + (b*m) - (d*m) - Math.sqrt(t)) / (1 + m**2),
                    (d + (a*m) + (b*m**2) - (m * Math.sqrt(t))) / (1 + m**2),
                    (a + (b*m) - (d*m) + Math.sqrt(t)) / (1 + m**2),
                    (d + (a*m) + (b*m**2) + (m * Math.sqrt(t))) / (1 + m**2)
            )
        }
        return intersecLine
    }

    /**
     * Returns the point in which two lines intersect
     *
     * x = (a - b) / (m2 - m1)
     * y = (am2 - bm1) / (m2 - m1)
     *
     * @param line1
     * @param line2
     * @return
     */
    static Point2D intersect(Line2D line1, Line2D line2) {
        Point2D point
        double a = getYcrossValue(line1)
        double b = getYcrossValue(line2)
        double m1 = getLineSlope(line1)
        double m2 = getLineSlope(line2)
        if(m1.isInfinite()) {
            point = new Point2D.Double(
                line1.x1,
                line2.y1
            )
        } else if(m2.isInfinite()) {
            point = new Point2D.Double(
                line2.x1,
                line1.y1
            )
        } else {
            point = new Point2D.Double(
                    (a - b) / (m2 - m1),
                    (a * m2 - b * m1) / (m2 - m1)
            )
        }
        return point
    }

    /**
     * Calculates the slop 'm' which represents a line in:
     *
     * y = mx + b
     *
     * @param line
     * @return
     */
    static double getLineSlope(Line2D line) {
        return (line.y2 - line.y1) / (line.x2 - line.x1)
    }

    /**
     * Returns the 'b' value in:
     *
     * y = mx + b
     *
     * @param line
     * @return
     */
    static double getYcrossValue(Line2D line) {
        return line.y1 - getLineSlope(line) * line.x1
    }
    /**
     * Return a resized version of a RectangularShape increasing/decreasing its size by N pixels
     * @param RectangularShape
     * @param sizeAdjuster : Width and Height modifier
     * @return
     */
    static Rectangle2D resizeRect(final RectangularShape rect, int sizeAdjuster) {
        return resizeRect(rect, sizeAdjuster, sizeAdjuster)
    }
    
    /**
     * Return a resized version of a RectangularShape in relation to its own size. eg. 1.5d will be 50% larger
     * @param rect
     * @param sizeAdjuster : pct to increase or reduce
     * @return
     */
    static Rectangle2D resizeRect(final RectangularShape rect, double sizeAdjuster) {
        int width = (rect.width * (sizeAdjuster - 1)).toFloat().round()
        int height = (rect.height * (sizeAdjuster - 1)).toFloat().round()
        return resizeRect(rect, width, height)
    }
    /**
     * Return a resized version of a RectangularShape increasing/decreasing its size by N pixels
     * @param CvRect
     * @param sizeAdjusterW : Width in pixels to add to width
     * @param sizeAdjusterH : Height in pixels to add to height
     * @param maxWidth : maximum width limit
     * @param maxHeight : maximum height limit
     * @return
     */
    static Rectangle2D resizeRect(final RectangularShape rect, int sizeAdjusterW, int sizeAdjusterH) {
        Rectangle2D resRect = rect as Rectangle2D
        if(sizeAdjusterW) {
            int rectX = (rect.x - (sizeAdjusterW * 0.5d)) as int
            int rectY = (rect.y - (sizeAdjusterH * 0.5d)) as int
            int rectW = (rect.width + sizeAdjusterW) as int
            int rectH = (rect.height + sizeAdjusterH) as int
            resRect = new Rectangle2D.Double(rectX, rectY, rectW, rectH)
        }
        return resRect
    }
    
    /**
     * Translate coordinates to top-left
     * @param rect : with coordinates in center
     * @return
     */
    static Rectangle2D coordsToTopLeft(Rectangle2D rect) {
        double radiusX = rect.width / 2d
        double radiusY = rect.height / 2d
        int top  = (rect.x - radiusX).toFloat().round()
        int left = (rect.y - radiusY).toFloat().round()
        return new Rectangle2D.Double(top, left, rect.width, rect.height)
    }
    /**
     * Translate coordinates to center
     * @param rect : with coordinates in top-left
     * @return
     */
    static Rectangle2D coordsToCenter(RectangularShape rect) {
        double radiusX = rect.width / 2d
        double radiusY = rect.height / 2d
        int centerX = (rect.x + radiusX).toFloat().round()
        int centerY = (rect.y + radiusY).toFloat().round()
        return new Rectangle2D.Double(centerX, centerY, rect.width, rect.height)
    }
    /**
     * Rotate a rectangle
     * @param rect : x,y its in center (use coordsToTopLeft if needed)
     * @param angle : clockwise in degrees (eg. 45)
     * @param pivot: if provided, will perform the rotation around that point (if not, around its own)
     *
     * https://stackoverflow.com/questions/4465931/rotate-rectangle-around-a-point
     * @return CvRect with coordinates in center
     */
    static Rectangle2D rotateRect(RectangularShape rect, int angle, Point2D pivot = null) {
        if(!pivot) {
            pivot = new Point2D.Double(rect.x + (rect.width / 2d).toFloat().round(), rect.y + (rect.height / 2d).toFloat().round())
        }
        double rad = angle * Math.PI/180
        double diffX = rect.x - pivot.x
        double diffY = rect.y - pivot.y
        
        int x = (rect.x - diffX + (diffX * Math.cos(rad)) + (diffY * Math.sin(rad))).toFloat().round()
        int y = (rect.y - diffY + (diffY * Math.cos(rad)) - (diffX * Math.sin(rad))).toFloat().round()
        return new Rectangle2D.Double(x, y, rect.width, rect.height)
    }
    
}
