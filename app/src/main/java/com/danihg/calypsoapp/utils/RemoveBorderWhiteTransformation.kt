package com.danihg.calypsoapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.sqrt

class RemoveBorderWhiteTransformation(
    private val tolerance: Int = 15  // How close a pixel’s color must be to the background to be removed.
) : Transformation {

    override val cacheKey: String
        get() = "RemoveBorderWhiteTransformation-$tolerance"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)

        // Assume the background color is the top-left corner.
        val bgColor = output.getPixel(0, 0)

        // Determine bounding box for the actual logo (ignoring white background)
        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = output.getPixel(x, y)
                if (colorDistance(pixel, bgColor) >= tolerance) { // Pixel is not part of the background
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        // Ensure cropping bounds are valid
        if (minX < maxX && minY < maxY) {
            return Bitmap.createBitmap(output, minX, minY, maxX - minX, maxY - minY)
        }

        return output // Return original if no significant difference
    }

    // Euclidean distance between two RGB colors.
    private fun colorDistance(c1: Int, c2: Int): Double {
        val r1 = Color.red(c1)
        val g1 = Color.green(c1)
        val b1 = Color.blue(c1)
        val r2 = Color.red(c2)
        val g2 = Color.green(c2)
        val b2 = Color.blue(c2)
        return sqrt(((r2 - r1) * (r2 - r1) +
                (g2 - g1) * (g2 - g1) +
                (b2 - b1) * (b2 - b1)).toDouble())
    }
}
