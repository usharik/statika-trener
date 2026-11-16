package cz.usharik.statika.practice

import cz.usharik.statika.learn.Pt
import cz.usharik.statika.learn.triGeom
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TriGeomTest {
    private fun dot(o: Pt, p: Pt, q: Pt) = (p.x - o.x) * (q.x - o.x) + (p.y - o.y) * (q.y - o.y)

    @Test
    fun pravyUhelADelkyPriKazdemOtoceni() {
        for (rot in 0..3) for (mirror in listOf(false, true)) {
            val g = triGeom(4.0, 3.0, scale = 10.0, rot = rot, mirror = mirror)
            assertTrue(abs(dot(g.c, g.a, g.b)) < 1e-6, "pravý úhel rot=$rot mirror=$mirror")
            assertEquals(40.0, hypot(g.a.x - g.c.x, g.a.y - g.c.y), 1e-6)
            assertEquals(30.0, hypot(g.b.x - g.c.x, g.b.y - g.c.y), 1e-6)
            assertEquals(50.0, hypot(g.b.x - g.a.x, g.b.y - g.a.y), 1e-6)
        }
    }

    @Test
    fun vejdeSeDoObdelniku() {
        for (rot in 0..3) for (mirror in listOf(false, true)) for ((a, o) in listOf(4.0 to 3.0, 12.0 to 5.0, 1.0 to 8.0, 2.0 to 2.0)) {
            val g = triGeom(a, o, rot = rot, mirror = mirror, fit = listOf(70.0, 30.0, 260.0, 160.0))
            assertTrue(g.minX >= 70 - 1e-6 && g.maxX <= 330 + 1e-6, "x $rot $mirror $a/$o: ${g.minX}..${g.maxX}")
            assertTrue(g.minY >= 30 - 1e-6 && g.maxY <= 190 + 1e-6, "y $rot $mirror $a/$o: ${g.minY}..${g.maxY}")
            assertTrue(abs(dot(g.c, g.a, g.b)) < 1e-6)
        }
    }

    @Test
    fun zakladniPoloha() {
        // α vlevo dole, pravý úhel vpravo dole, třetí vrchol nahoře (osa y dolů)
        val g = triGeom(4.0, 3.0, scale = 1.0)
        assertTrue(g.c.x > g.a.x && abs(g.c.y - g.a.y) < 1e-9)
        assertTrue(g.b.y < g.c.y && abs(g.b.x - g.c.x) < 1e-9)
    }
}
