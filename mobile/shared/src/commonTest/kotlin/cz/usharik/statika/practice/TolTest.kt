package cz.usharik.statika.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TolTest {
    @Test
    fun uhly() {
        assertTrue(accept("37", 37.0, 36.87, Tol.ANGLE))
        assertTrue(accept("36,9", 36.9, 36.87, Tol.ANGLE))
        assertFalse(accept("38", 38.0, 36.87, Tol.ANGLE))
        assertFalse(accept("0,64", 0.64, 36.87, Tol.ANGLE))
    }

    @Test
    fun hodnotyFunkci() {
        assertTrue(accept("0,87", 0.87, 0.866, Tol.RATIO))
        assertTrue(accept("0,866", 0.866, 0.8660254, Tol.RATIO))
        assertFalse(accept("0,5", 0.5, 0.866, Tol.RATIO))
        assertFalse(accept("0,85", 0.85, 0.866, Tol.RATIO))
    }

    @Test
    fun hodnotyAProcenta() {
        assertTrue(accept("8,66", 8.66, 8.660254, Tol.VALUE))
        assertTrue(accept("8,7", 8.7, 8.660254, Tol.VALUE)) // správně zaokrouhleno na 1 místo
        assertFalse(accept("8,5", 8.5, 8.660254, Tol.VALUE))
        assertTrue(accept("12", 12.0, 12.2, Tol.PERCENT))
        assertFalse(accept("13", 13.0, 12.2, Tol.PERCENT))
        assertTrue(accept("151", 151.0, 150.0, Tol.PERCENT))
    }

    @Test
    fun zlomky() {
        assertEquals(0.6, parseRatio("3/5"))
        assertEquals(0.75, parseRatio("3 : 4"))
        assertEquals(0.6, parseRatio("0,6"))
        assertNull(parseRatio("3/0"))
        assertNull(parseRatio("a/b"))
    }

    @Test
    fun presneHodnoty() {
        assertEquals(0.5, Trig.sinD(30.0))
        assertEquals(0.5, Trig.cosD(60.0))
        assertEquals(1.0, Trig.tgD(45.0))
        assertEquals(-0.5, Trig.sinD(210.0))
        assertEquals(-0.5, Trig.cosD(120.0))
        assertEquals(0.0, Trig.cosD(90.0))
        assertTrue(Trig.tgD(90.0).isNaN())
        assertEquals(36.87, Trig.atanD(0.75), 0.01)
    }
}
