package com.danihg.calypsoapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import coil.size.Size
import coil.transform.Transformation

class RemoveWhiteBackgroundTransformation : Transformation {
    override val cacheKey: String
        get() = "RemoveWhiteBackgroundTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Create a mutable copy of the input bitmap
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)

        // Replace pixels that are almost white with transparent pixels.
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = output.getPixel(x, y)
                // Check if the pixel is nearly white. You can adjust the threshold as needed.
                if (Color.red(pixel) > 240 && Color.green(pixel) > 240 && Color.blue(pixel) > 240) {
                    output.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        return output
    }
}