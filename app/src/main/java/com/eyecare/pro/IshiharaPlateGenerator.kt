package com.eyecare.pro

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Procedural, non-clinical pseudoisochromatic-style learning plate.
 *
 * Four ordered passes place large, medium, small, then micro dots.
 * Generation is called from Dispatchers.Default by the Compose layer;
 * this object never performs packing during Canvas drawing.
 */
internal object IshiharaPlateGenerator {

    internal data class Dot(
        val x: Float,
        val y: Float,
        val radius: Float,
        val color: Int
    )

    internal data class Palette(
        val numberColors: List<Int>,
        val backgroundColors: List<Int>
    ) {
        init {
            require(numberColors.isNotEmpty()) { "numberColors must not be empty" }
            require(backgroundColors.isNotEmpty()) { "backgroundColors must not be empty" }
        }
    }

    private data class Pass(
        val minimumRadiusRatio: Float,
        val maximumRadiusRatio: Float,
        val attempts: Int,
        val protectMaskEdge: Boolean
    )

    private data class Cell(val x: Int, val y: Int)

    private val passes = listOf(
        Pass(0.032f, 0.038f, 1_500, protectMaskEdge = true),
        Pass(0.020f, 0.026f, 3_000, protectMaskEdge = true),
        Pass(0.011f, 0.016f, 6_000, protectMaskEdge = false),
        Pass(0.005f, 0.008f, 10_000, protectMaskEdge = false)
    )

    /**
     * targetCount is retained for source compatibility with the previous
     * generator. The four specified passes, rather than targetCount, control
     * the number of placement attempts.
     */
    @Suppress("UNUSED_PARAMETER")
    internal fun generate(
        width: Int,
        height: Int,
        density: Float,
        number: String,
        palette: Palette,
        seed: Int,
        targetCount: Int = 420
    ): List<Dot> {
        if (width <= 0 || height <= 0 || number.isBlank()) return emptyList()

        val centerX = width / 2f
        val centerY = height / 2f
        // Keep the plate's geometry consistent with the Compose renderer (0.96 x min dimension).
        val plateRadius = minOf(width, height) * 0.48f
        if (plateRadius <= 0f) return emptyList()

        val mask = createNumberMask(width, height, centerX, centerY, plateRadius, number)
        val maskPixels = IntArray(width * height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        mask.recycle()

        val random = Random(seed)
        val dots = ArrayList<Dot>(passes.sumOf { it.attempts / 2 })
        val padding = 0.002f * plateRadius
        val largestRadius = passes.first().maximumRadiusRatio * plateRadius
        val cellSize = largestRadius * 2f + padding
        val grid = HashMap<Long, MutableList<Dot>>()

        for (pass in passes) {
            val minRadius = pass.minimumRadiusRatio * plateRadius
            val maxRadius = pass.maximumRadiusRatio * plateRadius

            repeat(pass.attempts) {
                val radius = minRadius + random.nextFloat() * (maxRadius - minRadius)
                val angle = random.nextFloat() * (2f * Math.PI.toFloat())
                val distance = sqrt(random.nextFloat()) * (plateRadius - radius)
                val x = centerX + distance * kotlin.math.cos(angle)
                val y = centerY + distance * kotlin.math.sin(angle)

                if (pass.protectMaskEdge &&
                    crossesMaskBoundary(x, y, radius, width, height, maskPixels)
                ) {
                    return@repeat
                }

                val cellX = floor(x / cellSize).toInt()
                val cellY = floor(y / cellSize).toInt()

                if (!hasSpace(x, y, radius, padding, cellX, cellY, grid)) {
                    return@repeat
                }

                val isNumber = maskAt(x, y, width, height, maskPixels)
                val colors = if (isNumber) palette.numberColors else palette.backgroundColors
                val dot = Dot(
                    x = x,
                    y = y,
                    radius = radius,
                    color = colors[random.nextInt(colors.size)]
                )

                dots.add(dot)
                grid.getOrPut(cellKey(cellX, cellY)) { ArrayList() }.add(dot)
            }
        }

        return dots
    }

    /**
     * A large/medium dot is accepted only if its center and four cardinal
     * edge samples all belong to the same side of the binary number mask.
     * This keeps large circles away from the number/background silhouette.
     */
    private fun crossesMaskBoundary(
        x: Float,
        y: Float,
        radius: Float,
        width: Int,
        height: Int,
        pixels: IntArray
    ): Boolean {
        val centerClass = maskAt(x, y, width, height, pixels)
        val sampleOffset = radius * 0.82f
        return maskAt(x, y - sampleOffset, width, height, pixels) != centerClass ||
            maskAt(x, y + sampleOffset, width, height, pixels) != centerClass ||
            maskAt(x - sampleOffset, y, width, height, pixels) != centerClass ||
            maskAt(x + sampleOffset, y, width, height, pixels) != centerClass
    }

    private fun maskAt(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        pixels: IntArray
    ): Boolean {
        val px = x.toInt().coerceIn(0, width - 1)
        val py = y.toInt().coerceIn(0, height - 1)
        return AndroidColor.alpha(pixels[py * width + px]) > 96
    }

    private fun createNumberMask(
        width: Int,
        height: Int,
        centerX: Float,
        centerY: Float,
        plateRadius: Float,
        number: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL_AND_STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = plateRadius * 0.035f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            // Requested nominal text size: 68% of the plate diameter.
            // A width cap prevents multi-character targets from being clipped.
            val nominal = (plateRadius * 2f) * 0.68f
            textSize = if (number.length >= 3) {
                minOf(nominal, plateRadius * 1.45f)
            } else {
                nominal
            }
        }

        val metrics = paint.fontMetrics
        val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(number, centerX, baseline, paint)
        return bitmap
    }

    /**
     * Spatial grid collision test. The cell size is at least the sum of
     * two maximum radii plus padding, so the surrounding 3x3 cells suffice.
     */
    private fun hasSpace(
        x: Float,
        y: Float,
        radius: Float,
        padding: Float,
        cellX: Int,
        cellY: Int,
        grid: Map<Long, List<Dot>>
    ): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val neighbors = grid[cellKey(cellX + dx, cellY + dy)] ?: continue
                for (other in neighbors) {
                    val deltaX = x - other.x
                    val deltaY = y - other.y
                    val minimumDistance = radius + other.radius + padding
                    if (deltaX * deltaX + deltaY * deltaY <
                        minimumDistance * minimumDistance
                    ) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun cellKey(x: Int, y: Int): Long {
        return (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
    }

    internal fun defaultPalette(): Palette = Palette(
        numberColors = listOf(
            AndroidColor.rgb(190, 76, 48),
            AndroidColor.rgb(215, 105, 60),
            AndroidColor.rgb(174, 85, 55),
            AndroidColor.rgb(226, 130, 80),
            AndroidColor.rgb(155, 68, 47)
        ),
        backgroundColors = listOf(
            AndroidColor.rgb(105, 139, 78),
            AndroidColor.rgb(139, 153, 81),
            AndroidColor.rgb(164, 169, 112),
            AndroidColor.rgb(112, 137, 103),
            AndroidColor.rgb(180, 174, 132)
        )
    )

    internal fun alternatePalette(): Palette = Palette(
        numberColors = listOf(
            AndroidColor.rgb(190, 85, 70),
            AndroidColor.rgb(220, 115, 80),
            AndroidColor.rgb(170, 65, 55),
            AndroidColor.rgb(230, 145, 100)
        ),
        backgroundColors = listOf(
            AndroidColor.rgb(100, 130, 100),
            AndroidColor.rgb(140, 155, 100),
            AndroidColor.rgb(170, 170, 120),
            AndroidColor.rgb(110, 145, 125)
        )
    )
}
