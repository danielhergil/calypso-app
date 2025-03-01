package com.danihg.calypsoapp.sources

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import android.view.MotionEvent
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.facedetector.FaceDetectorCallback


class CameraCalypsoSource(context: Context) : VideoSource() {

    private val camera = CameraCalypsoApiManager(context)
    private var facing = CameraHelper.Facing.BACK

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        val result = checkResolutionSupported(width, height)
        if (!result) {
            throw IllegalArgumentException("Unsupported resolution: ${width}x$height")
        }
        return true
    }

    override fun start(surfaceTexture: SurfaceTexture) {
        this.surfaceTexture = surfaceTexture
        if (!isRunning()) {
            surfaceTexture.setDefaultBufferSize(width, height)
            camera.prepareCamera(surfaceTexture, width, height, fps, facing)
            camera.openCameraFacing(facing)
        }
    }

    override fun stop() {
        if (isRunning()) camera.closeCamera()
    }

    override fun release() {}

    override fun isRunning(): Boolean = camera.isRunning

    private fun checkResolutionSupported(width: Int, height: Int): Boolean {
        if (width % 2 != 0 || height % 2 != 0) {
            throw IllegalArgumentException("width and height values must be divisible by 2")
        }
        val size = Size(width, height)
        val resolutions = if (facing == CameraHelper.Facing.BACK) {
            camera.cameraResolutionsBack
        } else camera.cameraResolutionsFront
        return if (camera.levelSupported == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            //this is a wrapper of camera1 api. Only listed resolutions are supported
            resolutions.contains(size)
        } else {
            val widthList = resolutions.map { size.width }
            val heightList = resolutions.map { size.height }
            val maxWidth = widthList.maxOrNull() ?: 0
            val maxHeight = heightList.maxOrNull() ?: 0
            val minWidth = widthList.minOrNull() ?: 0
            val minHeight = heightList.minOrNull() ?: 0
            size.width in minWidth..maxWidth && size.height in minHeight..maxHeight
        }
    }

    fun switchCamera() {
        facing = if (facing == CameraHelper.Facing.BACK) {
            CameraHelper.Facing.FRONT
        } else {
            CameraHelper.Facing.BACK
        }
        if (isRunning()) {
            stop()
            surfaceTexture?.let {
                start(it)
            }
        }
    }

    fun getCameraFacing(): CameraHelper.Facing = facing

    fun getCameraResolutions(facing: CameraHelper.Facing): List<Size> {
        val resolutions = if (facing == CameraHelper.Facing.FRONT) {
            camera.cameraResolutionsFront
        } else {
            camera.cameraResolutionsBack
        }
        return resolutions.toList()
    }

    fun setExposure(level: Int) {
        if (isRunning()) camera.exposure = level
    }

    fun getExposure(): Int {
        return if (isRunning()) camera.exposure else 0
    }

    fun setSensorExposureTime(time: Long) {
        if (isRunning()) camera.setSensorExposureTime(time)
    }

    fun setExposureCompensation(compensation: Int) {
        if (isRunning()) {
            camera.exposureCompensation = compensation
        }
    }

    // --- NEW: White Balance support ---
    fun setWhiteBalance(mode: Int) {
        if (isRunning()) camera.whiteBalance = mode
    }

    fun getWhiteBalance(): Int {
        return if (isRunning()) camera.whiteBalance else CaptureRequest.CONTROL_AWB_MODE_AUTO
    }

    // Inside CameraCalypsoSource.kt
    fun setManualWhiteBalance(temperature: Float) {
        if (isRunning()) {
            camera.setManualWhiteBalance(temperature)
        }
    }
    // ----------------------------------


    fun enableLantern() {
        if (isRunning()) camera.enableLantern()
    }

    fun disableLantern() {
        if (isRunning()) camera.disableLantern()
    }

    fun isLanternEnabled(): Boolean {
        return if (isRunning()) camera.isLanternEnabled else false
    }

    fun enableAutoFocus(): Boolean {
        if (isRunning()) return camera.enableAutoFocus()
        return false
    }

    fun disableAutoFocus(): Boolean {
        if (isRunning()) return camera.disableAutoFocus()
        return false
    }

    fun isAutoFocusEnabled(): Boolean {
        return if (isRunning()) camera.isAutoFocusEnabled else false
    }

    fun tapToFocus(event: MotionEvent): Boolean {
        return camera.tapToFocus(event)
    }

    @JvmOverloads
    fun setZoom(event: MotionEvent, delta: Float = 0.1f) {
        if (isRunning()) camera.setZoom(event, delta)
    }

    fun setZoom(level: Float) {
        if (isRunning()) camera.zoom = level
    }

    fun getZoomRange(): Range<Float> = camera.zoomRange

    fun getZoom(): Float = camera.zoom

    fun enableFaceDetection(callback: FaceDetectorCallback): Boolean {
        return if (isRunning()) camera.enableFaceDetection(callback) else false
    }

    fun disableFaceDetection() {
        if (isRunning()) camera.disableFaceDetection()
    }

    fun isFaceDetectionEnabled() = camera.isFaceDetectionEnabled()

    fun camerasAvailable(): Array<String> = camera.camerasAvailable

    fun getCurrentCameraId() = camera.getCurrentCameraId()

    fun openCameraId(id: String) {
        if (isRunning()) camera.reOpenCamera(id)
    }

    fun enableOpticalVideoStabilization(): Boolean {
        return if (isRunning()) camera.enableOpticalVideoStabilization() else false
    }

    fun disableOpticalVideoStabilization() {
        if (isRunning()) camera.disableOpticalVideoStabilization()
    }

    fun isOpticalVideoStabilizationEnabled() = camera.isOpticalStabilizationEnabled

    fun enableVideoStabilization(): Boolean {
        return if (isRunning()) camera.enableVideoStabilization() else false
    }

    fun disableVideoStabilization() {
        if (isRunning()) camera.disableVideoStabilization()
    }

    fun isVideoStabilizationEnabled() = camera.isVideoStabilizationEnabled

    fun enableAutoExposure(): Boolean {
        return if (isRunning()) camera.enableAutoExposure() else false
    }

    fun disableAutoExposure() {
        if (isRunning()) camera.disableAutoExposure()
    }

    fun isAutoExposureEnabled() = camera.isAutoExposureEnabled

    @JvmOverloads
    fun addImageListener(format: Int, maxImages: Int, autoClose: Boolean = true, listener: CameraCalypsoApiManager.ImageCallback) {
        val w = if (rotation == 90 || rotation == 270) height else width
        val h = if (rotation == 90 || rotation == 270) width else height
        camera.addImageListener(w, h, format, maxImages, autoClose, listener)
    }

    fun removeImageListener() {
        camera.removeImageListener()
    }
}
