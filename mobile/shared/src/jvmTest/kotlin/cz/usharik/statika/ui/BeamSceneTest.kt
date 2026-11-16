package cz.usharik.statika.ui

import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.SupportType
import cz.usharik.statika.model.generate
import cz.usharik.statika.model.solveProblem
import cz.usharik.statika.trainer.DrawingView
import kotlin.test.Test
import kotlin.test.assertTrue

class BeamSceneTest {
    private fun keyedLines(scene: Scene, key: String) =
        scene.prims.filterIsInstance<Prim.Line>().filter { (it.anim as? Anim.Keyed)?.key == key }

    @Test
    fun vodorovnaReakceVetknutiNesmeriDoZdi() {
        var checked = 0
        for (seed in 1..300) {
            val p = generate(seed, GenOptions(1, listOf(BeamType.CANTILEVER)))
            val s = p.supports.single { it.type == SupportType.FIXED }
            val scene = buildScene(p, setOf(s.id), emptySet(), null, DrawingView(), null, false, null, LightColors)
            val w = scene.width
            val m = 130.0
            val xs = m + s.x * (w - 2 * m) / p.length
            val line = keyedLines(scene, "r_${s.id}x").single()
            val left = s.x < p.length / 2
            // zeď je vlevo u levého vetknutí, vpravo u pravého – šipka musí být na straně nosníku
            if (left) assertTrue(minOf(line.x1, line.x2) >= xs - 1, "úloha $seed: šipka vlevo od vetknutí (ve zdi)")
            else assertTrue(maxOf(line.x1, line.x2) <= xs + 1, "úloha $seed: šipka vpravo od vetknutí (ve zdi)")
            checked++
        }
        assertTrue(checked == 300)
    }

    @Test
    fun nulovaReakceVeVysledkuNemaSipku() {
        for (seed in 1..100) {
            val p = generate(seed, GenOptions(1, listOf(BeamType.SIMPLE)))
            val r = solveProblem(p)!!
            val scene = buildScene(p, emptySet(), emptySet(), null, DrawingView(), r, false, null, LightColors)
            for ((id, v) in r) if (v == 0.0) assertTrue(keyedLines(scene, "r_$id-res").isEmpty(), "úloha $seed: nulová $id má šipku")
        }
    }
}
