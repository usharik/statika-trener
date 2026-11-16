package cz.usharik.statika.practice

import cz.usharik.statika.model.fmt
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Konkrétní typy úloh: typická chyba se musí poznat SVÝM kódem (žák dostane tu správnou radu),
 * a vlastnosti, které obecný test neumí vyjádřit (počty možností, rozsahy, úrovně).
 */
class ExerciseTypesTest {
    private fun first(type: String, level: Int, want: (Exercise) -> Boolean): Exercise =
        (1..2000).asSequence().map { Exercises.make(type, it, level) }.first(want)

    private fun inputs(ex: Exercise, vararg v: Double?): List<String> =
        ex.fields.mapIndexed { i, f -> fmt(v.getOrNull(i) ?: f.answer, 3).replace('−', '-') }

    private fun codeFor(ex: Exercise, vararg v: Double?): String? = check(ex, inputs(ex, *v)).trap

    @Test
    fun vsechnyTypyJsouVRejstrikuASouboryJsouUplne() {
        assertEquals((1..23).map { "E" + it.toString().padStart(2, '0') }, Exercises.all.map { it.id }.filter { it != "E17S" })
        // série po lekci o znaménkách: složky se znaménkem už na lehké úrovni
        assertTrue(Exercises.make("E17S", 5, 1).fields.all { it.signed })
        for (set in PracticeSets.all) for (level in 1..3) assertTrue(set.available(level).isNotEmpty(), "${set.id}/$level je prázdný")
        // každý typ, který soubor uvádí, existuje
        for (set in PracticeSets.all) for ((id, _) in set.types) assertTrue(Exercises.byId.containsKey(id), "${set.id}: $id")
    }

    @Test
    fun prohozenySinACosSePozna() {
        val ex = first("E03", 2) { it.traps.any { t -> t.code == "SWAP" } }
        val swap = ex.traps.first { it.code == "SWAP" }
        assertEquals("SWAP", codeFor(ex, swap.wrong[0]))
    }

    @Test
    fun znamenkoSlozkySePozna() {
        val ex = first("E17", 2) { abs(it.fields[0].answer) > 1 && abs(it.fields[1].answer) > 1 && abs(abs(it.fields[0].answer) - abs(it.fields[1].answer)) > 1 }
        assertEquals("SIGN_X", codeFor(ex, -ex.fields[0].answer, ex.fields[1].answer))
        assertEquals("SIGN_Y", codeFor(ex, ex.fields[0].answer, -ex.fields[1].answer))
    }

    @Test
    fun podilMistoUhluSePozna() {
        val ex = first("E13", 1) { it.fields[0].answer > 20 }
        val q = Trig.tgD(ex.fields[0].answer)
        assertEquals("RATIO", codeFor(ex, q))
        assertEquals("COMPL", codeFor(ex, 90 - ex.fields[0].answer))
    }

    @Test
    fun procentaJakoStupneSePoznaji() {
        val ex = first("E14", 1) { it.prompt.cs.contains("%") && it.fields[0].tol == Tol.ANGLE }
        val p = Regex("má sklon ([0-9]+) %").find(ex.prompt.cs)!!.groupValues[1].toDouble()
        assertEquals("PCT_DEG", codeFor(ex, p))
    }

    @Test
    fun soucetMistoPythagorySePozna() {
        val ex = first("E19", 1) { true }
        val (x, y) = Regex("= (−?[0-9]+) kN").findAll(ex.prompt.cs).map { it.groupValues[1].replace('−', '-').toDouble() }.toList()
        assertEquals("SUM", codeFor(ex, abs(x) + abs(y), null))
        assertEquals("NOSQRT", codeFor(ex, x * x + y * y, null))
    }

    @Test
    fun celaPakaMistoRamenaSePozna() {
        val ex = first("E21", 2) { it.prompt.cs.contains("30°") || it.prompt.cs.contains("45°") }
        val f = Regex("síla ([0-9]+) kN").find(ex.prompt.cs)!!.groupValues[1].toDouble()
        val r = Regex("dlouhé ([0-9,]+) m").find(ex.prompt.cs)!!.groupValues[1].replace(',', '.').toDouble()
        assertEquals("ARM_FULL", codeFor(ex, null, f * r))
    }

    @Test
    fun volbyMajiPravidelnyPocetMoznosti() {
        for (seed in 1..300) {
            assertEquals(3, Exercises.make("E01", seed, 2).choice!!.size)
            assertEquals(4, Exercises.make("E06", seed, 2).choice!!.size)
            assertEquals(4, Exercises.make("E16", seed, 1).choice!!.size)
            assertEquals(3, Exercises.make("E08", seed, 1).choice!!.size)
            assertEquals(3, Exercises.make("E08", seed, 3).choice!!.size)
        }
    }

    @Test
    fun rezimKalkulackyMaVzdyJednoznacnouOdpoved() {
        // displej musí odlišit správný režim od ostatních (jinak by žák nemohl odpovědět)
        for (seed in 1..500) for (level in 1..3) {
            val ex = Exercises.make("E08", seed, level)
            val shown = Regex("= (−?[0-9,]+)\\.").find(ex.prompt.cs)!!.groupValues[1]
            val wrongWithSameValue = ex.choice!!.filter { !it.correct }.count { o -> o.why!!.cs.contains("dalo $shown.") || o.why!!.cs.contains("přesně $shown.") }
            assertEquals(0, wrongWithSameValue, "E08/$level/$seed: jiný režim dává stejné číslo $shown")
        }
    }

    @Test
    fun urovenUlohOdpovidaRozsahu() {
        for (seed in 1..300) {
            // úroveň 1 je bez kalkulačky: složky jen pro 30/45/60 a úloha dává hodnoty sin/cos
            val e17 = Exercises.make("E17", seed, 1)
            assertTrue(e17.prompt.cs.contains("sin ") && e17.fields.none { it.signed } && e17.prompt.cs.contains("bez znamének"), "E17/1/$seed")
            val e23 = Exercises.make("E23", seed, 1)
            assertTrue(e23.prompt.cs.contains("cos "), "E23/1/$seed: chybí hodnoty funkcí")
            // úhly ze stran jsou ostré
            val e12 = Exercises.make("E12", seed, 3)
            assertTrue(e12.fields[0].answer in 0.0..90.0, "E12/3/$seed")
        }
    }

    @Test
    fun maleUhlyMajiPrisnejsiToleranci() {
        // sklon 2 % = 1,15°: dříve prošlo cokoli mezi 0,65° a 1,65°
        assertTrue(accept("1,1", 1.1, 1.146, Tol.ANGLE))
        assertTrue(accept("1,2", 1.2, 1.146, Tol.ANGLE))
        assertTrue(!accept("1,4", 1.4, 1.146, Tol.ANGLE))
        assertTrue(!accept("0,8", 0.8, 1.146, Tol.ANGLE))
        // velké úhly beze změny (±0,5°)
        assertTrue(accept("37", 37.0, 36.87, Tol.ANGLE))
    }

    @Test
    fun malePrevyseniANeprijmeVodorovnouDelku() {
        // E15: převýšení 0,1 m nesmí přijmout 0,08; šikmá délka nesmí přijmout vodorovnou délku
        assertTrue(!accept("0,08", 0.08, 0.1, Tol.REL))
        assertTrue(accept("0,1", 0.1, 0.1, Tol.REL))
        for (seed in 1..300) for (level in 2..3) {
            val ex = Exercises.make("E15", seed, level)
            val d = Regex("délku ([0-9]+) m").find(ex.prompt.cs)!!.groupValues[1].toDouble()
            assertTrue(!check(ex, listOf(fmt(ex.fields[0].answer, 3), fmt(d, 0))).solved, "E15/$level/$seed: vodorovná délka prošla jako šikmá")
        }
    }

    @Test
    fun soucetSlozekToleruje_zaokrouhleneSlozky() {
        // E22: 20 · cos 30° ≈ 17,32 – kdo počítá s 0,87, dostane 17,4 − 10 = 7,4 místo 7,32; to je zaokrouhlení, ne chyba
        val ex = (1..3000).asSequence().map { Exercises.make("E22", it, 2) }.first { e ->
            e.prompt.cs.contains("[[F_1]] = 20 kN") && e.prompt.cs.contains("30°") && kotlin.math.abs(e.fields[0].answer) > 1
        }
        val f = ex.fields[0]
        assertTrue(accept(fmt(f.answer + 0.08, 2), f.answer + 0.08, f.answer, f.tol, f.scale))
    }

    @Test
    fun podilVsRadUMalychUhlu() {
        // sklon 1 : 20: arctg 0,05 v RAD ≈ 0,05 → rada musí zmínit i režim RAD
        val ex = (1..3000).asSequence().map { Exercises.make("E13", it, 3) }.first { it.prompt.cs.contains("1 : 20") }
        val r = check(ex, listOf("0,05"))
        assertEquals("RATIO", r.trap)
        val text = ex.traps.first { it.code == "RATIO" }.msg.cs
        assertTrue(text.contains("RAD"), text)
    }

    @Test
    fun nazvyStranNapovedaUPreponyNeradiPrilehlou() {
        // druhá nápověda nesmí u přepony vést k odpovědi „přilehlá“ (přepona také vychází z vrcholu α)
        val ex = (1..500).asSequence().map { Exercises.make("E01", it, 1) }.first { e -> e.ok.cs.contains("přepona") }
        assertTrue(ex.hints[1].cs.contains("naproti pravému úhlu je přepona"))
    }

    @Test
    fun zlomekUPodiluProjdeAJinyZapisDostaneRadu() {
        val ex = (1..500).asSequence().map { Exercises.make("E02", it, 1) }.first { it.fields[0].answer == 0.6 }
        assertTrue(check(ex, listOf("3/5")).solved)
        val e3 = Exercises.make("E03", 1, 2)
        val r = check(e3, listOf("5 m"))
        assertEquals("NOTNUM", r.trap)
        assertTrue(!r.counted)
    }
}
