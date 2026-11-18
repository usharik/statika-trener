package cz.usharik.statika.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.text.font.createFontFamilyResolver
import cz.usharik.statika.i18n.Lang
import cz.usharik.statika.i18n.Tr
import cz.usharik.statika.model.BeamType
import cz.usharik.statika.model.GenOptions
import cz.usharik.statika.model.generate
import cz.usharik.statika.platform.rasterize
import cz.usharik.statika.trainer.fullSolution
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProblemPdfTest {
    private val measurer = pdfTextMeasurer(createFontFamilyResolver())

    @Test
    fun reseniSpocitaVsechnyReakceStejneJakoSoustava() {
        for (t in BeamType.entries) for (level in 1..3) for (seed in 1..150) {
            val p = generate(seed, GenOptions(level, listOf(t)))
            val sol = fullSolution(p)
            for (l in sol.lines) assertEquals(sol.values.getValue(l.u.id), l.value)
            assertTrue(sol.coupled.isEmpty(), "úloha $seed (${t.key}, $level): soustava ${sol.coupled}")
            assertEquals(sol.values.keys, sol.lines.map { it.u.id }.toSet(), "úloha $seed")
        }
    }

    @Test
    fun pdfZadaniIReseni() {
        // náhledy stránek: PDF_PREVIEW=1 ./gradlew :shared:jvmTest --tests '*ProblemPdfTest*'
        val out = System.getenv("PDF_PREVIEW")?.let { File("build/pdf-preview").apply { mkdirs() } }
        for (t in BeamType.entries) for (lang in Lang.entries) {
            val p = generate(7, GenOptions(3, listOf(t)))
            for (withSolution in listOf(false, true)) {
                val doc = problemPdf(p, 3, withSolution, Tr(lang), measurer)
                if (withSolution) assertTrue(doc.pages.size in 1..3, "${t.key}: ${doc.pages.size} stran")
                else assertEquals(1, doc.pages.size)
                doc.pages.indices.forEach { i ->
                    val img = doc.rasterize(i, 2f)
                    if (out != null) {
                        val png = Image.makeFromBitmap(img.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
                        File(out, "${t.key}-${lang.code}-${if (withSolution) "reseni" else "zadani"}-${i + 1}.png").writeBytes(png)
                    }
                }
            }
        }
    }
}
