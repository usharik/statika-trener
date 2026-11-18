package cz.usharik.statika.platform

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/** Nakreslí stránku [page] do [canvas], kde jeden bod stránky má [unitsPerPoint] jednotek plátna (PDF: 1). */
fun PdfDoc.drawPage(page: Int, canvas: Canvas, unitsPerPoint: Float) {
    canvas.save()
    canvas.scale(unitsPerPoint / PdfDoc.SCALE, unitsPerPoint / PdfDoc.SCALE)
    CanvasDrawScope().draw(Density(PdfDoc.SCALE), LayoutDirection.Ltr, canvas, Size(PdfDoc.WIDTH * PdfDoc.SCALE, PdfDoc.HEIGHT * PdfDoc.SCALE)) {
        pages[page](this)
    }
    canvas.restore()
}

/** Stránka [page] jako obrázek ([scale] pixelů na bod) s bílým pozadím – pro platformy bez vektorového PDF z Compose. */
fun PdfDoc.rasterize(page: Int, scale: Float): ImageBitmap {
    val img = ImageBitmap((PdfDoc.WIDTH * scale).roundToInt(), (PdfDoc.HEIGHT * scale).roundToInt())
    val canvas = Canvas(img)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(img.width.toFloat(), img.height.toFloat())) { drawRect(Color.White) }
    drawPage(page, canvas, scale)
    return img
}
