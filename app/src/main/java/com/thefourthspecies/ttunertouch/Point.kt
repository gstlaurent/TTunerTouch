package com.thefourthspecies.ttunertouch

/**
 * Point: represents a location in two ways:
 *  1. clock: modified polar coordinates based
 *      - middle of view is origin (centerX, centerY)
 *      - vertical is 0
 *      - @position: a number from 0 to 1 representing ratio between vertical and 360 degrees
 *          - theta = offset - angle
 *                offset: since starting at 90 degrees instead of at 0
 *                (-): since measuring clockwise instead of counterclockwise
 * 2. screen coordinates: top left of the screen is (0, 0) and they increase positively down and to the right
 *  Basic, pre-modified formulae:
 *    x = r * cos(theta)
 *    y = r * sin(theta)
 *
 *
 *
 */
class Point {
    var position: Double = 0.0
        private set
    var distance: Float = 0.0f
        private set
    var x: Float = 0.0f
        private set
    var y: Float = 0.0f
        private set
    val screenAngle: Float
        get() {
            val theta: Float = ((position - 0.25) * 360).toFloat()
            return if (theta < 0) { theta + 360f } else theta
        }

    companion object {
        fun polar(position: Double, distance: Float, centerX: Float, centerY: Float): Point {
            val point = Point()
            val theta: Double = Math.PI / 2.0 - (2.0 * Math.PI * position)
            point.position = position
            point.distance = distance
            point.x = point.calcX(theta, distance, centerX)
            point.y = point.calcY(theta, distance, centerY)
            return point
        }

        fun screen(x: Float, y: Float, centerX: Float, centerY: Float): Point {
            val point = Point()
            val xx = (x - centerX).toDouble()
            val yy = (centerY - y).toDouble()

            point.position = point.calcPosition(yy, xx)
            point.distance = Math.sqrt(xx * xx + yy * yy).toFloat()
            point.x = x
            point.y = y
            return point
        }
    }

    private fun calcPosition(yy: Double, xx: Double): Double {
        val theta = Math.atan2(yy, xx)
        val pos = (Math.PI / 2 - theta) / (Math.PI * 2)
        val posBounded = when {
            pos < 0 -> pos + 1
            pos > 1 -> pos - 1
            else -> pos
        }
        return posBounded
    }

    private fun calcX(theta: Double, distance: Float, centerX: Float): Float {
        val xx = distance * Math.cos(theta).toFloat()
        return centerX + xx.toFloat()
    }

    private fun calcY(theta: Double, distance: Float, centerY: Float): Float {
        val yy = distance * Math.sin(theta).toFloat()
        return centerY - yy.toFloat()
    }
}

//
//        private set
//    var screenAngle: Float = 0.0f
//        private set
//
//
//
//    companion object {
//        fun polar(position: Double, distance: Float, centerX: Float, centerY: Float): Point {
//            val point = Point()
//            val theta: Double = Math.PI / 2.0 - (2.0 * Math.PI * position)
//            point.position = position
//            point.distance = distance
//            point.x = point.calcX(theta, distance, centerX)
//            point.y = point.calcY(theta, distance, centerY)
//            point.screenAngle = 360f - theta.toFloat()
//            return point
//        }
//
//        fun screen(x: Float, y: Float, centerX: Float, centerY: Float): Point {
//            val point = Point()
//            val xx = (x - centerX).toDouble()
//            val yy = (centerY - y).toDouble()
//            val theta = Math.atan2(yy, xx)
//
//            point.position = point.calcPosition(theta)
//            point.distance = Math.sqrt(xx * xx + yy * yy).toFloat()
//            point.x = x
//            point.y = y
//            point.screenAngle = 360f - (theta * (360.0/2*Math.PI)).toFloat()
//            return point
//        }
//    }

