package com.danihg.calypsoapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.sqrt

class RemoveBorderWhiteTransformation(
    private val borderSize: Int = 10,   // How many pixels inward to check from each edge.
    private val tolerance: Int = 15     // How close a pixel’s color must be to the background to be removed.
) : Transformation {

    override val cacheKey: String
        get() = "RemoveBorderWhiteTransformation-$borderSize-$tolerance"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height

        // Assume the background color is the top-left corner.
        val bgColor = output.getPixel(0, 0)

        // Loop only over the border region.
        for (x in 0 until width) {
            for (y in 0 until height) {
                // Check if the pixel is within the border region.
                if (x < borderSize || y < borderSize || x > width - borderSize || y > height - borderSize) {
                    val pixel = output.getPixel(x, y)
                    if (colorDistance(pixel, bgColor) < tolerance) {
                        output.setPixel(x, y, Color.TRANSPARENT)
                    }
                }
            }
        }
        return output
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