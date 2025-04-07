// CameraViewModel.kt
package com.danihg.calypsoapp.presentation.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import com.danihg.calypsoapp.sources.CameraCalypsoSource

class CameraViewModel(context: Context) : ViewModel() {
    // Create and hold a single instance of the camera source.
    val activeCameraSource: CameraCalypsoSource = CameraCalypsoSource(context)
}
