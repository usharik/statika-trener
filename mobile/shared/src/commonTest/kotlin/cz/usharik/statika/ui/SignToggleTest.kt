package cz.usharik.statika.ui

import cz.usharik.statika.model.parseNum
import kotlin.test.Test
import kotlin.test.assertEquals

class SignToggleTest {
    @Test
    fun plusMinusPrepinaZnamenko() {
        assertEquals("−3,5", toggleSign("3,5"))
        assertEquals("3,5", toggleSign("−3,5"))
        assertEquals("3,5", toggleSign("-3,5"))
        assertEquals("3,5", toggleSign("–3,5"))
        assertEquals("−", toggleSign(""))
        assertEquals("", toggleSign("−"))
        // přepnuté číslo se dá vyhodnotit
        assertEquals(-4.0, parseNum(toggleSign("4")))
        assertEquals(1.75, parseNum(toggleSign(toggleSign("1,75"))))
    }
}
