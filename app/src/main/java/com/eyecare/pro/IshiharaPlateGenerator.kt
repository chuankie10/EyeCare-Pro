package com.eyecare.pro

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Procedural, non-clinical pseudoisochromatic-style learning plate.
 * Coordinates and colors are generated before Compose Canvas drawing.
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
            require(numberColors.isNotEmpty())
            require(backgroundColors.isNotEmpty())
        }
    }

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
        val plateRadius = minOf(width, height) * 0.46f
        if (plateRadius <= 0f) return emptyList()

        val mask = createNumberMask(width, height, centerX, centerY, plateRadius, number)
        val random = Random(seed)
        val dots = ArrayList<Dot>(targetCount)

        val minRadius = 2.0f * density
        val maxRadius = 7.0f * density
        val gap = 0.8f * density
        val cellSize = maxRadius * 2f + gap
        val grid = HashMap<Long, MutableList<Dot>>()
        val maxAttempts = targetCount * 45

        try {
            var attempts = 0
            while (dots.size < targetCount && attempts < maxAttempts) {
                attempts++

                val radius = randomRadius(random, density)
                val angle = random.nextFloat() * (2f * Math.PI.toFloat())
                val distance = sqrt(random.nextFloat()) * (plateRadius - radius)
                val x = centerX + distance * kotlin.math.cos(angle)
                val y = centerY + distance * kotlin.math.sin(angle)

                val cellX = floor(x / cellSize).toInt()
                val cellY = floor(y / cellSize).toInt()

                if (!hasSpace(x, y, radius, gap, cellX, cellY, cellSize, grid)) {
                    continue
                }

                val px = x.toInt().coerceIn(0, width - 1)
                val py = y.toInt().coerceIn(0, height - 1)
                val isNumber = AndroidColor.alpha(mask.getPixel(px, py)) > 96
                val colors = if (isNumber) palette.numberColors else palette.backgroundColors
                val dot = Dot(x, y, radius, colors[random.nextInt(colors.size)])

                dots.add(dot)
                grid.getOrPut(cellKey(cellX, cellY)) { ArrayList() }.add(dot)
            }
        } finally {
            mask.recycle()
        }

        return dots
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
            style = Paint.Style.FILL
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
            textSize = plateRadius * if (number.length >= 3) 0.78f else 1.05f
        }
        val metrics = paint.fontMetrics
        val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(number, centerX, baseline, paint)
        return bitmap
    }

    private fun randomRadius(random: Random, density: Float): Float {
        return when (random.nextInt(100)) {
            in 0..54 -> (2.0f + random.nextFloat() * 1.6f) * density
            in 55..89 -> (3.6f + random.nextFloat() * 1.6f) * density
            else -> (5.2f + random.nextFloat() * 1.8f) * density
        }
    }

    private fun hasSpace(
        x: Float,
        y: Float,
        radius: Float,
        gap: Float,
        cellX: Int,
        cellY: Int,
        cellSize: Float,
        grid: Map<Long, List<Dot>>
    ): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                val neighbors = grid[cellKey(cellX + dx, cellY + dy)] ?: continue
                for (other in neighbors) {
                    val deltaX = x - other.x
                    val deltaY = y - other.y
                    val minimumDistance = radius + other.radius + gap
                    if (deltaX * deltaX + deltaY * deltaY < minimumDistance * minimumDistance) {
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
