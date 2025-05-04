package com.danihg.calypsoapp.presentation.camera

import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.library.generic.GenericStream

@Composable
fun CameraPreview(
    genericStream: GenericStream,
    surfaceViewRef: MutableState<SurfaceView?>,
) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {


                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceViewRef.value = this

                holder.setFormat(PixelFormat.TRANSLUCENT)

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        if (!genericStream.isOnPreview) {
                            genericStream.startPreview(this@apply)
                        }
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        genericStream.getGlInterface().setPreviewResolution(width, height)
                    }
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        if (genericStream.isOnPreview) {
                            genericStream.stopPreview()
                        }
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}