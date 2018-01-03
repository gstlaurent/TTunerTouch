package com.thefourthspecies.ttunertouch

import org.junit.Test

import org.junit.Assert.*

/**
 * Created by Graham on 2017-12-31.
 */
class UIUnitTests {
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

    fun testPolarToScreen(position: Double, distance: Float, centerX: Float, centerY: Float) {
        var pp = Point.polar(position, distance, centerX, centerY)
        var ps = Point.screen(pp.x, pp.y, centerX, centerY)
        assertEquals(pp.position, ps.position, dDELTA)
        assertEquals(pp.distance, ps.distance, fDELTA)
    }

    fun testScreenToPolar(x: Float, y: Float, centerX: Float, centerY: Float) {
        var ps = Point.screen(x, y, centerX, centerY)
        var pp = Point.polar(ps.position, ps.distance, centerX, centerY)
        assertEquals(ps.x, pp.x, fDELTA)
        assertEquals(ps.y, pp.y, fDELTA)
    }
}


