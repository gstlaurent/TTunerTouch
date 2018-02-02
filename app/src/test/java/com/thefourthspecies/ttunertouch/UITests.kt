package com.thefourthspecies.ttunertouch

import org.junit.Test
import org.junit.Assert.*

/**
 * Created by Graham on 2017-12-31.
 */
class UITests {
    val dDELTA = 1e-7
    val fDELTA = 1e-7f

    @Test
    fun pointConversion() {
        testPolarToScreen(0.0, 200f, 300f, 400f)
        testPolarToScreen(0.25, 100f, 100f, 100f)
        testPolarToScreen(0.5, 1f, 0f, 0f)
        testPolarToScreen(0.75, 0.1f, 1f, 1000f)
        testPolarToScreen(0.99, 200f, 300f, 400f)

        testScreenToPolar(200f, 100f, 200f, 200f)
        testScreenToPolar(300f, 200f, 200f, 200f)
        testScreenToPolar(200f, 300f, 200f, 200f)
        testScreenToPolar(100f, 200f, 200f, 200f)
        testScreenToPolar(0f, 0f, 200f, 200f)


    }

    @Test
    fun pointMoves() {
        Point.center(250f, 260f)
        var paPosition = Point.polar(0.33, 300f)
        paPosition.moveByPosition(0.10)
        var pePosition = Point.polar(0.10, 300f)
        assertEquals(paPosition, pePosition)

        var paDistance = Point.polar(0.33, 300f)
        paDistance.moveByDistance(400f)
        var peDistance = Point.polar(0.33, 400f)
        assertEquals(paDistance, peDistance)

        var paX = Point.screen(120f, 300f)
        paX.moveByX(1f)
        var peX = Point.screen(1f, 300f)
        assertEquals(paX, peX)

        var paY = Point.screen(120f, 300f)
        paY.moveByY(37f)
        var peY = Point.screen(120f, 37f)
        assertEquals(paY, peY)
    }

    fun testPolarToScreen(position: Double, distance: Float, centerX: Float, centerY: Float) {
        Point.center(centerX, centerY)
        var pp = Point.polar(position, distance)
        var ps = Point.screen(pp.x, pp.y)
        assertEquals(pp.position, ps.position, dDELTA)
        assertEquals(pp.distance, ps.distance, fDELTA)
    }

    fun testScreenToPolar(x: Float, y: Float, centerX: Float, centerY: Float) {
        Point.center(centerX, centerY)
        var ps = Point.screen(x, y)
        var pp = Point.polar(ps.position, ps.distance)
        assertEquals(ps.x, pp.x, fDELTA)
        assertEquals(ps.y, pp.y, fDELTA)
    }
}


