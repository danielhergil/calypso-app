package com.danihg.calypsoapp.sources

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size
import android.view.MotionEvent
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.facedetector.FaceDetectorCallback

/**
 * An improved CameraCalypsoSource that allows the user to set manual camera parameters
 * (manual exposure, ISO, white balance, etc.) individually.
 *
 * It saves manual values so that they can be re‑applied if the camera is restarted (for example,
 * after a lifecycle change).
 */
class CameraCalypsoSource(context: Context) : VideoSource() {

    // Use the improved API manager (which now supports ISO, exposure compensation, etc.)
    private val camera = CameraCalypsoApiManager(context, this)
    private var facing = CameraHelper.Facing.BACK

    // Saved manual parameters
    private var savedExposure: Int? = null
    private var savedSensorExposureTime: Long? = null
    private var savedSensorSensitivity: Int? = null  // ISO value
    private var savedExposureCompensation: Int? = null // New: saved exposure compensation (e.g., -2 to 2)
    private var savedWhiteBalanceMode: Int? = null     // e.g., CONTROL_AWB_MODE_AUTO or CONTROL_AWB_MODE_OFF
    private var savedManualWhiteBalanceTemperature: Float? = null

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        if (!checkResolutionSupported(width, height)) {
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
            throw IllegalArgumentException("Width and height must be divisible by 2")
        }
        val size = Size(width, height)
        val resolutions = if (facing == CameraHelper.Facing.BACK) {
            camera.cameraResolutionsBack
        } else {
            camera.cameraResolutionsFront
        }
        return if (camera.levelSupported == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            resolutions.contains(size)
        } else {
            val widthList = resolutions.map { it.width }
            val heightList = resolutions.map { it.height }
            val minWidth = widthList.minOrNull() ?: 0
            val maxWidth = widthList.maxOrNull() ?: 0
            val minHeight = heightList.minOrNull() ?: 0
            val maxHeight = heightList.maxOrNull() ?: 0
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
            surfaceTexture?.let { start(it) }
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

    // ─── MANUAL EXPOSURE ─────────────────────────────────────────────

    /** Set manual exposure compensation (if not using sensor exposure time). */
    fun setExposure(level: Int) {
        savedExposure = level
        if (isRunning()) {
            camera.exposure = level
        }
    }

    fun getExposure(): Int = if (isRunning()) camera.exposure else 0

    /** Set manual sensor exposure time (shutter speed). */
    fun setSensorExposureTime(time: Long) {
        savedSensorExposureTime = time
        if (isRunning()) {
            camera.setSensorExposureTime(time)
        }
    }

    // ─── MANUAL ISO (SENSOR SENSITIVITY) ─────────────────────────────

    fun setSensorSensitivity(iso: Int) {
        savedSensorSensitivity = iso
        if (isRunning()) {
            camera.setSensorSensitivity(iso)
        }
    }

    fun getSensorSensitivity(): Int = if (isRunning()) camera.getSensorSensitivity() else 0

    fun getSupportedISORange(): Range<Int>? = camera.getSupportedISORange()

    fun setIsoAuto() {
        savedSensorSensitivity = null
        // Optionally notify the API manager to re-enable auto mode.
        if (isRunning()) {
            camera.enableAutoExposure() // Or a dedicated function if available.
        }
    }

    fun setSensorExposureAuto() {
        savedSensorExposureTime = null
        if (isRunning()) {
            camera.enableAutoExposure()
        }
    }

    /** Returns true if both ISO and sensor exposure time are set to auto */
    fun isExposureCompensationAvailable(): Boolean {
        return savedSensorExposureTime == null || savedSensorSensitivity == null
    }

    // ─── MANUAL EXPOSURE COMPENSATION ───────────────────────────────
    /**
     * Set the manual exposure compensation value.
     * Standard values (for many cameras) are -2, -1, 0, 1, 2.
     */
    fun setExposureCompensation(compensation: Int) {
        camera.setExposureCompensationManual(compensation)
    }

    fun getExposureCompensation(): Int {
        return camera.getExposureCompensationManual()
    }

    fun setExposureCompensationManual(compensation: Int) {
        camera.setExposureCompensationManual(compensation)
    }

    fun getExposureCompensationManual(): Int {
        return camera.getExposureCompensationManual()
    }

    // ─── MANUAL WHITE BALANCE ─────────────────────────────────────────

    fun setWhiteBalance(mode: Int) {
        savedWhiteBalanceMode = mode
        if (isRunning()) {
            camera.whiteBalance = mode
        }
    }

    fun getWhiteBalance(): Int = if (isRunning()) camera.whiteBalance else CaptureRequest.CONTROL_AWB_MODE_AUTO

    /** Set manual white balance using a temperature value (in Kelvin). */
    fun setManualWhiteBalance(temperature: Float) {
        savedWhiteBalanceMode = CaptureRequest.CONTROL_AWB_MODE_OFF
        savedManualWhiteBalanceTemperature = temperature
        if (isRunning()) {
            camera.setManualWhiteBalance(temperature)
        }
    }

    // ─── REAPPLY MANUAL SETTINGS ──────────────────────────────────────

    /**
     * Reapplies any saved manual settings (sensor exposure time, ISO, exposure compensation, white balance)
     * to the current camera session. This should be called (for example) after the app resumes.
     */
    fun reapplySettings() {
        if (!isRunning()) return
        // Reapply sensor exposure or exposure compensation.
        savedSensorExposureTime?.let {
            disableAutoExposure()
            camera.setSensorExposureTime(it)
        } ?: run {
            savedExposure?.let { camera.exposure = it }
        }
        // Reapply ISO if set.
        savedSensorSensitivity?.let { camera.setSensorSensitivity(it) }
        // Reapply exposure compensation.
        savedExposureCompensation?.let { camera.exposureCompensation = it }
        // Reapply white balance.
        savedWhiteBalanceMode?.let { mode ->
            if (mode == CaptureRequest.CONTROL_AWB_MODE_AUTO) {
                camera.whiteBalance = CaptureRequest.CONTROL_AWB_MODE_AUTO
            } else {
                savedManualWhiteBalanceTemperature?.let { temperature ->
                    setManualWhiteBalance(temperature)
                }
            }
        }
    }

    // ─── OTHER CONTROLS ────────────────────────────────────────────────

    fun enableLantern() { if (isRunning()) camera.enableLantern() }
    fun disableLantern() { if (isRunning()) camera.disableLantern() }
    fun isLanternEnabled(): Boolean = if (isRunning()) camera.isLanternEnabled else false

    fun enableAutoFocus(): Boolean = if (isRunning()) camera.enableAutoFocus() else false
    fun disableAutoFocus(): Boolean = if (isRunning()) camera.disableAutoFocus() else false
    fun isAutoFocusEnabled(): Boolean = if (isRunning()) camera.isAutoFocusEnabled else false

    fun tapToFocus(event: MotionEvent): Boolean = camera.tapToFocus(event)

    @JvmOverloads
    fun setZoom(event: MotionEvent, delta: Float = 0.1f) { if (isRunning()) camera.setZoom(event, delta) }
    fun setZoom(level: Float) { if (isRunning()) camera.zoom = level }
    fun getZoomRange(): Range<Float> = camera.zoomRange
    fun getZoom(): Float = camera.zoom

    fun enableFaceDetection(callback: FaceDetectorCallback): Boolean = if (isRunning()) camera.enableFaceDetection(callback) else false
    fun disableFaceDetection() { if (isRunning()) camera.disableFaceDetection() }
    fun isFaceDetectionEnabled() = camera.isFaceDetectionEnabled()

    fun camerasAvailable(): Array<String> = camera.camerasAvailable
    fun getCurrentCameraId() = camera.getCurrentCameraId()
    fun openCameraId(id: String) { if (isRunning()) camera.reOpenCamera(id) }

    fun enableOpticalVideoStabilization(): Boolean = if (isRunning()) camera.enableOpticalVideoStabilization() else false
    fun disableOpticalVideoStabilization() { if (isRunning()) camera.disableOpticalVideoStabilization() }
    fun isOpticalVideoStabilizationEnabled() = camera.isOpticalStabilizationEnabled

    fun enableVideoStabilization(): Boolean = if (isRunning()) camera.enableVideoStabilization() else false
    fun disableVideoStabilization() { if (isRunning()) camera.disableVideoStabilization() }
    fun isVideoStabilizationEnabled() = camera.isVideoStabilizationEnabled

    fun enableAutoExposure(): Boolean = if (isRunning()) camera.enableAutoExposure() else false
    fun disableAutoExposure() { if (isRunning()) camera.disableAutoExposure() }
    fun isAutoExposureEnabled() = camera.isAutoExposureEnabled

    @JvmOverloads
    fun addImageListener(format: Int, maxImages: Int, autoClose: Boolean = true, listener: CameraCalypsoApiManager.ImageCallback) {
        val w = if (rotation == 90 || rotation == 270) height else width
        val h = if (rotation == 90 || rotation == 270) width else height
        camera.addImageListener(w, h, format, maxImages, autoClose, listener)
    }

    fun removeImageListener() { camera.removeImageListener() }
}
