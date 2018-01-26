package com.thefourthspecies.ttunertouch

import android.util.Log
import kotlin.math.roundToInt

/**
 * Point: represents a location in two ways:
 *  1. Polar: modified polar coordinates based on Clock coordinates
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
class Point private constructor() {

    var position: Double = 0.0
        private set(position) {
            assert(0 <= position && position < 1.0) {
                "$this: 'position' must be in range [0-1)."
            }
            field = position
        }

    var distance: Float = 0f
        private set

    var x: Float = 0f
        get() {
            if (needsScreenRefresh()) {
                refreshScreenCoordinates()
            }
            return field
        }
        private set

    var y: Float = 0f
        get() {
            if (needsScreenRefresh()) {
                refreshScreenCoordinates()
            }
            return field
        }
        private set

    val screenAngle: Float
        get() {
            val theta: Float = ((position - 0.25) * 360).toFloat()
            return if (theta < 0) { theta + 360f } else theta
        }

    private var version = 0
    private fun needsScreenRefresh(): Boolean = version != Point.version

    fun moveByPosition(position: Double) {
        this.position = position
        refreshScreenCoordinates()
    }

    fun moveByDistance(distance: Float) {
        this.distance = distance
        refreshScreenCoordinates()
    }

    fun moveByX(x: Float) {
        this.x = x
        refreshPolarCoordinates()
    }

    fun moveByY(y: Float) {
        this.y = y
        refreshPolarCoordinates()
    }

    private fun refreshPolarCoordinates() {
        val xx = (x - centerX).toDouble()
        val yy = (centerY - y).toDouble()
        position = calcPosition(yy, xx)
        distance = Math.sqrt(xx * xx + yy * yy).toFloat()
    }

    private fun refreshScreenCoordinates() {
        val theta: Double = Math.PI / 2.0 - (2.0 * Math.PI * position)
        x = calcX(theta, distance, centerX)
        y = calcY(theta, distance, centerY)
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

    override fun toString(): String {
        return "Point(position=$position, distance=$distance, x=$x, y=$y)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Point) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = centerX.hashCode()
        result = 31 * result + centerY.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    companion object {
        var centerX: Float = 0f
            private set
        var centerY: Float = 0f
            private set

        @Synchronized fun center(x: Float, y: Float) {
            centerX = x
            centerY = y
            version++
        }

        private var version = 0

        fun polar(position: Double, distance: Float): Point {
            val point = Point()
            point.version = version
            point.position = position
            point.distance = distance
            point.refreshScreenCoordinates()
            return point
        }

        fun screen(x: Float, y: Float): Point {
            val point = Point()
            point.version = version
            point.x = x
            point.y = y
            point.refreshPolarCoordinates()
            return point
        }
    }
}

// An alternative (broken) implementation that tries to remove floating point problems:
//
//const val dPRECISION = 1e-7
//const val fPRECISION = 1e-7f
//
//class Point(val centerX: Float = 0f, val centerY: Float = 0f) {
//
//    var _position: Int = 0
//    var position: Double
//        get() = _position * dPRECISION
//        private set(position) {
//            assert(0 <= position && position < (1.0 - dPRECISION)) {
//                "$this: 'position' must be in range [0-1). Tolerance: $dPRECISION"
//            }
//            _position = (position / dPRECISION).roundToInt()
//        }
//
//    var _distance: Int = 0
//    var distance: Float
//        get() = _distance * fPRECISION
//        private set(distance) {
//            _distance = (distance / fPRECISION).roundToInt()
//        }
//
//    var _x: Int = 0
//    var x: Float
//        get() = _x * fPRECISION
//        private set(x) {
//            _x = (x / fPRECISION).roundToInt()
//        }
//
//    var _y: Int = 0
//    var y: Float
//        get() = _y * fPRECISION
//        private set(y) {
//            _y = (y / fPRECISION).roundToInt()
//        }
//
//    val screenAngle: Float
//        get() {
//            val theta: Float = ((position - 0.25) * 360).toFloat()
//            return if (theta < 0) { theta + 360f } else theta
//        }
//
//    fun moveByPosition(position: Double) {
//        this.position = position
//        refreshScreenCoordinates()
//    }
//
//    fun moveByDistance(distance: Float) {
//        this.distance = distance
//        refreshScreenCoordinates()
//    }
//
//    fun moveByX(x: Float) {
//        this.x = x
//        refreshPolarCoordinates()
//    }
//
//    fun moveByY(y: Float) {
//        this.y = y
//        refreshPolarCoordinates()
//    }
//
//    private fun refreshPolarCoordinates() {
//        val xx = (x - centerX).toDouble()
//        val yy = (centerY - y).toDouble()
//        position = calcPosition(yy, xx)
//        distance = Math.sqrt(xx * xx + yy * yy).toFloat()
//    }
//
//    private fun refreshScreenCoordinates() {
//        val theta: Double = Math.PI / 2.0 - (2.0 * Math.PI * position)
//        x = calcX(theta, distance, centerX)
//        y = calcY(theta, distance, centerY)
//    }
//
//    private fun calcPosition(yy: Double, xx: Double): Double {
//        val theta = Math.atan2(yy, xx)
//        val pos = (Math.PI / 2 - theta) / (Math.PI * 2)
//        val posBounded = when {
//            pos < 0 -> pos + 1
//            pos > 1 -> pos - 1
//            else -> pos
//        }
//        return posBounded
//    }
//
//    private fun calcX(theta: Double, distance: Float, centerX: Float): Float {
//        val xx = distance * Math.cos(theta).toFloat()
//        return centerX + xx.toFloat()
//    }
//
//    private fun calcY(theta: Double, distance: Float, centerY: Float): Float {
//        val yy = distance * Math.sin(theta).toFloat()
//        return centerY - yy.toFloat()
//    }
//
//
//    override fun toString(): String {
//        return "Point(position=$position, distance=$distance, x=$x, y=$y)"
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Point) return false
//
//        if (centerX != other.centerX) return false
//        if (centerY != other.centerY) return false
//        if (_position != other._position) return false
//        if (_distance != other._distance) return false
//        if (_x != other._x) return false
//        if (_y != other._y) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = centerX.hashCode()
//        result = 31 * result + centerY.hashCode()
//        result = 31 * result + _position
//        result = 31 * result + _distance
//        result = 31 * result + _x
//        result = 31 * result + _y
//        return result
//    }
//
//    companion object {
//        fun polar(position: Double, distance: Float, centerX: Float, centerY: Float): Point {
//            val point = Point(centerX, centerY)
//            point.position = position
//            point.distance = distance
//            point.refreshScreenCoordinates()
//            return point
//        }
//
//        fun screen(x: Float, y: Float, centerX: Float, centerY: Float): Point {
//            val point = Point(centerX, centerY)
//            point.x = x
//            point.y = y
//            point.refreshPolarCoordinates()
//            return point
//        }
//    }
//}

