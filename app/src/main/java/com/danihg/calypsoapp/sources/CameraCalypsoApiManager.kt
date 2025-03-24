package com.danihg.calypsoapp.sources

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import com.pedro.common.secureGet
import com.pedro.encoder.input.video.Camera2ResolutionCalculator.getOptimalResolution
import com.pedro.encoder.input.video.CameraCallbacks
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraHelper.Facing
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.encoder.input.video.facedetector.FaceDetectorCallback
import com.pedro.encoder.input.video.facedetector.mapCamera2Faces
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.math.max
import kotlin.math.min

/**
 * An improved CameraCalypsoApiManager that exposes additional manual controls including:
 *
 * • Manual shutter speed (sensor exposure time)
 * • Manual ISO (sensor sensitivity)
 * • Manual white balance (with custom temperature)
 *
 * It also provides a helper to reapply all manual settings when needed.
 */
@SuppressLint("MissingPermission")
class CameraCalypsoApiManager(context: Context, private val owner: CameraCalypsoSource? = null) : CameraDevice.StateCallback() {

    private val TAG = "CameraCalypsoApiManager"

    private var cameraDevice: CameraDevice? = null
    private var surfaceEncoder = Surface(SurfaceTexture(-1).apply { release() })
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraHandler: Handler? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var isPrepared: Boolean = false
    private var cameraId: String = "0"
    private var facing = Facing.BACK
    private var builderInputSurface: CaptureRequest.Builder? = null
    private var fingerSpacing = 0f
    private var zoomLevel = 0f
    var isLanternEnabled: Boolean = false
        private set
    var isVideoStabilizationEnabled: Boolean = false
        private set
    var isOpticalStabilizationEnabled: Boolean = false
        private set
    var isAutoFocusEnabled: Boolean = true
        private set
    var isAutoExposureEnabled: Boolean = false
        private set
    var isRunning: Boolean = false
        private set
    private var fps = 30
    private val semaphore = Semaphore(0)
    private var cameraCallbacks: CameraCallbacks? = null

    interface ImageCallback {
        fun onImageAvailable(image: Image)
    }

    private var sensorOrientation = 0
    private var faceSensorScale: Rect? = null
    private var faceDetectorCallback: FaceDetectorCallback? = null
    private var faceDetectionEnabled = false
    private var faceDetectionMode = 0
    private var imageReader: ImageReader? = null

    // --- Saved Manual Parameters for reapplication ---
    // Exposure (shutter speed) and ISO (sensor sensitivity) are stored.
    var savedSensorExposureTime: Long? = null
    var savedSensorSensitivity: Int? = null
    var savedWhiteBalanceMode: Int? = null
    var savedManualWhiteBalanceTemperature: Float? = null
    var savedExposureCompensation: Int = 0  // New: Exposure compensation in EV steps (e.g., -2 to +2)

    init {
        cameraId = try { getCameraIdForFacing(Facing.BACK) } catch (e: Exception) { "0" }
    }

    fun prepareCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int, fps: Int) {
        val optimalResolution = getOptimalResolution(Size(width, height), getCameraResolutions(facing))
        Log.i(TAG, "optimal resolution set to: ${optimalResolution.width}x${optimalResolution.height}")
        surfaceTexture.setDefaultBufferSize(optimalResolution.width, optimalResolution.height)
        this.surfaceEncoder = Surface(surfaceTexture)
        this.fps = fps
        isPrepared = true
    }

    fun prepareCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int, fps: Int, facing: Facing) {
        this.facing = facing
        prepareCamera(surfaceTexture, width, height, fps)
    }

    fun prepareCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int, fps: Int, cameraId: String) {
        this.cameraId = cameraId
        this.facing = getFacingByCameraId(cameraManager, cameraId)
        prepareCamera(surfaceTexture, width, height, fps)
    }

    fun prepareCamera(surface: Surface, fps: Int) {
        this.surfaceEncoder = surface
        this.fps = fps
        isPrepared = true
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        try {
            val listSurfaces = mutableListOf<Surface>()
            listSurfaces.add(surfaceEncoder)
            imageReader?.let { listSurfaces.add(it.surface) }
            val captureRequest = drawSurface(cameraDevice, listSurfaces)
            createCaptureSession(
                cameraDevice,
                listSurfaces,
                onConfigured = {
                    cameraCaptureSession = it
                    try {
                        it.setRepeatingRequest(captureRequest, if (faceDetectionEnabled) cb else null, cameraHandler)
                    } catch (e: IllegalStateException) {
                        reOpenCamera(cameraId)
                    } catch (e: Exception) {
                        cameraCallbacks?.onCameraError("Create capture session failed: ${e.message}")
                        Log.e(TAG, "Error", e)
                    }
                },
                onConfiguredFailed = {
                    it.close()
                    cameraCallbacks?.onCameraError("Configuration failed")
                    Log.e(TAG, "Configuration failed")
                },
                cameraHandler
            )
        } catch (e: IllegalStateException) {
            reOpenCamera(cameraId)
        } catch (e: Exception) {
            cameraCallbacks?.onCameraError("Create capture session failed: ${e.message}")
            Log.e(TAG, "Error", e)
        }
    }

    @Throws(IllegalStateException::class, Exception::class)
    private fun drawSurface(cameraDevice: CameraDevice, surfaces: List<Surface>): CaptureRequest {
        val builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        for (surface in surfaces) builder.addTarget(surface)
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val validFps = min(60, fps)
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(validFps, validFps))
        this.builderInputSurface = builder
        return builder.build()
    }

    fun getSupportedFps(size: Size?, facing: Facing): List<Range<Int>> {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(getCameraIdForFacing(facing))
            val fpsSupported = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: return emptyList()
            return if (size != null) {
                val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val list = mutableListOf<Range<Int>>()
                val fd = streamConfigurationMap?.getOutputMinFrameDuration(SurfaceTexture::class.java, size) ?: return emptyList()
                val maxFPS = (10f / "0.$fd".toFloat()).toInt()
                for (r in fpsSupported) {
                    if (r.upper <= maxFPS) {
                        list.add(r)
                    }
                }
                list
            } else fpsSupported.toList()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error", e)
            return emptyList()
        }
    }

    val levelSupported: Int
        get() {
            val characteristics = cameraCharacteristics ?: return -1
            return characteristics.secureGet(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1
        }

    fun openCameraBack() {
        openCameraFacing(Facing.BACK)
    }

    fun openCameraFront() {
        openCameraFacing(Facing.FRONT)
    }

    fun openLastCamera() {
        openCameraId(cameraId)
    }

    fun setCameraId(cameraId: String) {
        this.cameraId = cameraId
    }

    var cameraFacing: Facing
        get() = facing
        set(cameraFacing) {
            try {
                val camId = getCameraIdForFacing(cameraFacing)
                facing = cameraFacing
                this.cameraId = camId
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }

    val cameraResolutionsBack: Array<Size>
        get() = getCameraResolutions(Facing.BACK)

    val cameraResolutionsFront: Array<Size>
        get() = getCameraResolutions(Facing.FRONT)

    fun getCameraResolutions(facing: Facing): Array<Size> = getCameraResolutions(getCameraIdForFacing(facing))

    fun getCameraResolutions(cameraId: String): Array<Size> {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = characteristics.secureGet(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java) ?: arrayOf()
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            arrayOf()
        }
    }

    val cameraCharacteristics: CameraCharacteristics?
        get() {
            return try {
                cameraManager.getCameraCharacteristics(cameraId)
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                null
            }
        }

    fun enableAutoExposure(): Boolean {
        val characteristics = cameraCharacteristics ?: return false
        val builder = builderInputSurface ?: return false
        val modes = characteristics.secureGet(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: return false
        if (!modes.contains(CaptureRequest.CONTROL_AE_MODE_ON)) return false
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        isAutoExposureEnabled = true
        return true
    }

    fun disableAutoExposure() {
        val characteristics = cameraCharacteristics ?: return
        val builder = builderInputSurface ?: return
        val modes = characteristics.secureGet(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: return
        if (!modes.contains(CaptureRequest.CONTROL_AE_MODE_ON)) return
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        isAutoExposureEnabled = false
    }

    fun enableVideoStabilization(): Boolean {
        val characteristics = cameraCharacteristics ?: return false
        val builder = builderInputSurface ?: return false
        val modes = characteristics.secureGet(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: return false
        if (!modes.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)) return false
        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)
        isVideoStabilizationEnabled = true
        return true
    }

    fun disableVideoStabilization() {
        val characteristics = cameraCharacteristics ?: return
        val builder = builderInputSurface ?: return
        val modes = characteristics.secureGet(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: return
        if (!modes.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON)) return
        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
        isVideoStabilizationEnabled = false
    }

    fun enableOpticalVideoStabilization(): Boolean {
        val characteristics = cameraCharacteristics ?: return false
        val builder = builderInputSurface ?: return false
        val modes = characteristics.secureGet(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: return false
        if (!modes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)) return false
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
        isOpticalStabilizationEnabled = true
        return true
    }

    fun disableOpticalVideoStabilization() {
        val characteristics = cameraCharacteristics ?: return
        val builder = builderInputSurface ?: return
        val modes = characteristics.secureGet(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: return
        if (!modes.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)) return
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
        isOpticalStabilizationEnabled = false
    }

    fun setFocusDistance(distance: Float) {
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        try {
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, max(0f, distance))
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    fun getCurrentCameraId() = cameraId

    var exposure: Int
        get() {
            val builder = builderInputSurface ?: return 0
            return builder.secureGet(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ?: 0
        }
        set(value) {
            val characteristics = cameraCharacteristics ?: return
            val builder = builderInputSurface ?: return
            val session = cameraCaptureSession ?: return
            val range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return
            val v = value.coerceIn(range.lower, range.upper)
            try {
                builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, v)
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }

    // --- Manual White Balance support ---
    var whiteBalance: Int
        get() {
            val builder = builderInputSurface ?: return CaptureRequest.CONTROL_AWB_MODE_AUTO
            return builder.secureGet(CaptureRequest.CONTROL_AWB_MODE) ?: CaptureRequest.CONTROL_AWB_MODE_AUTO
        }
        set(value) {
            val characteristics = cameraCharacteristics ?: return
            val builder = builderInputSurface ?: return
            val session = cameraCaptureSession ?: return
            val supportedWB = characteristics.secureGet(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) ?: return
            if (!supportedWB.contains(value)) {
                Log.e(TAG, "White balance mode $value is not supported")
                return
            }
            try {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, value)
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting white balance", e)
            }
        }

    fun setManualWhiteBalance(temperature: Float) {
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return

        // Save manual white balance parameters.
        savedWhiteBalanceMode = CaptureRequest.CONTROL_AWB_MODE_OFF
        savedManualWhiteBalanceTemperature = temperature

        // Disable auto white balance.
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
        // Set color correction mode.
        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
        // Calculate and set gains.
        val gains = calculateGainsFromTemperature(temperature)
        builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
        // Set an identity transform.
        val identityMatrix = ColorSpaceTransform(arrayOf(
            Rational(1, 1), Rational(0, 1), Rational(0, 1),
            Rational(0, 1), Rational(1, 1), Rational(0, 1),
            Rational(0, 1), Rational(0, 1), Rational(1, 1)
        ))
        builder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, identityMatrix)
        try {
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting manual white balance", e)
        }
    }

    private fun calculateGainsFromTemperature(temperature: Float): RggbChannelVector {
        // Clamp to a reasonable range.
        val temp = temperature.coerceIn(2000f, 8000f)
        // Simple linear mapping (approximate)
        val redMultiplier = 2.0f - (temp - 2000f) / 6000f * 1.5f  // ~2.0 to ~0.5
        val blueMultiplier = 0.5f + (temp - 2000f) / 6000f * 1.5f  // ~0.5 to ~2.0
        return RggbChannelVector(redMultiplier, 1.0f, 1.0f, blueMultiplier)
    }
    // ----------------------------------

    // --- Manual ISO support (SENSOR_SENSITIVITY) ---
    fun setSensorSensitivity(iso: Int) {
        // Disable auto exposure so manual settings can take effect.
        builderInputSurface?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        // Set the manual ISO value.
        builderInputSurface?.set(CaptureRequest.SENSOR_SENSITIVITY, iso)
        try {
            cameraCaptureSession?.setRepeatingRequest(
                builderInputSurface!!.build(),
                if (faceDetectionEnabled) cb else null,
                null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sensor sensitivity", e)
        }
    }

    fun getSensorSensitivity(): Int {
        // Return a default value (e.g., 100) if not set
        return builderInputSurface?.secureGet(CaptureRequest.SENSOR_SENSITIVITY) ?: 100
    }

    fun getSupportedISORange(): Range<Int>? {
        val characteristics = cameraCharacteristics ?: return null
        return characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
    }
    // ----------------------------------

    // --- Reapply all saved manual settings ---
    fun reapplyManualSettings() {
        // Reapply sensor exposure time (including compensation)
        savedSensorExposureTime?.let {
            // Apply exposure compensation on top of the base exposure time.
            val factor = Math.pow(2.0, savedExposureCompensation.toDouble())
            val finalExposureTime = (it * factor).toLong()
            setSensorExposureTime(finalExposureTime)
        }
        // Reapply ISO if set.
        savedSensorSensitivity?.let { setSensorSensitivity(it) }
        // Reapply manual white balance if needed.
        if (savedWhiteBalanceMode == CaptureRequest.CONTROL_AWB_MODE_OFF) {
            savedManualWhiteBalanceTemperature?.let { setManualWhiteBalance(it) }
        } else {
            whiteBalance = CaptureRequest.CONTROL_AWB_MODE_AUTO
        }
    }
    // ----------------------------------

    // Hold the base sensor exposure time.
    private var defaultSensorExposureTime: Long = 16666667L

    fun setSensorExposureTime(time: Long) {
        // Save the last manual exposure but leave the default unchanged.
        savedSensorExposureTime = time
        // Note: Do not update defaultSensorExposureTime here.
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        // Disable AE before setting manual exposure.
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, time)
        try {
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sensor exposure time", e)
        }
    }

    /**
     * Sets manual exposure compensation in EV steps.
     * This computes a new sensor exposure time as:
     *      newExposureTime = defaultSensorExposureTime * 2^(compensation)
     * and then applies that value.
     */
    fun setExposureCompensationManual(compensation: Int) {
        savedExposureCompensation = compensation
        // Calculate factor. For example, if compensation = 1, factor=2; if -1, factor=0.5.
        val factor = Math.pow(2.0, compensation.toDouble())
        // Use the unchanged default sensor exposure time (set when auto-exposure was on)
        val newExposureTime = (defaultSensorExposureTime * factor).toLong()

        // Disable auto exposure
        builderInputSurface?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        // Clear any previous exposure compensation
        builderInputSurface?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
        // Set the new sensor exposure time
        builderInputSurface?.set(CaptureRequest.SENSOR_EXPOSURE_TIME, newExposureTime)

        try {
            cameraCaptureSession?.setRepeatingRequest(builderInputSurface!!.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting exposure compensation manually", e)
        }
    }

    fun getExposureCompensationManual(): Int {
        return savedExposureCompensation
    }

    var exposureCompensation: Int
        get() {
            val builder = builderInputSurface ?: return 0
            return builder.secureGet(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ?: 0
        }
        set(value) {
            val characteristics = cameraCharacteristics ?: return
            val range = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return
            val comp = value.coerceIn(range.lower, range.upper)
            builderInputSurface?.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, comp)
            try {
                cameraCaptureSession?.setRepeatingRequest(
                    builderInputSurface!!.build(),
                    if (faceDetectionEnabled) cb else null,
                    null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting exposure compensation", e)
            }
        }

    val maxExposure: Int
        get() {
            val characteristics = cameraCharacteristics ?: return 0
            val supportedExposure = characteristics.secureGet(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.upper ?: 0
            return supportedExposure
        }

    val minExposure: Int
        get() {
            val characteristics = cameraCharacteristics ?: return 0
            val supportedExposure = characteristics.secureGet(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.lower ?: 0
            return supportedExposure
        }

    fun tapToFocus(event: MotionEvent): Boolean {
        val builder = builderInputSurface ?: return false
        val session = cameraCaptureSession ?: return false
        var result = false
        val pointerId = event.getPointerId(0)
        val pointerIndex = event.findPointerIndex(pointerId)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        if (x < 100 || y < 100) return false
        val touchRect = Rect((x - 100).toInt(), (y - 100).toInt(), (x + 100).toInt(), (y + 100).toInt())
        val focusArea = MeteringRectangle(touchRect, MeteringRectangle.METERING_WEIGHT_DONT_CARE)
        try {
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusArea))
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
            isAutoFocusEnabled = true
            result = true
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
        return result
    }

    fun openCameraFacing(selectedCameraFacing: Facing) {
        try {
            val camId = getCameraIdForFacing(selectedCameraFacing)
            openCameraId(camId)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    val isLanternSupported: Boolean
        get() {
            val characteristics = cameraCharacteristics ?: return false
            return characteristics.secureGet(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        }

    @Throws(Exception::class)
    fun enableLantern() {
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        if (isLanternSupported) {
            try {
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
                isLanternEnabled = true
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        } else {
            Log.e(TAG, "Lantern unsupported")
            throw Exception("Lantern unsupported")
        }
    }

    fun disableLantern() {
        val characteristics = cameraCharacteristics ?: return
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        val available = characteristics.secureGet(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: return
        if (available) {
            try {
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
                isLanternEnabled = false
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }
    }

    fun enableAutoFocus(): Boolean {
        var result = false
        val characteristics = cameraCharacteristics ?: return false
        val supportedFocusModes = characteristics.secureGet(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: return false
        val builder = builderInputSurface ?: return false
        val session = cameraCaptureSession ?: return false
        try {
            if (supportedFocusModes.isNotEmpty()) {
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
                if (supportedFocusModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    isAutoFocusEnabled = true
                } else if (supportedFocusModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO)) {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                    isAutoFocusEnabled = true
                } else {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, supportedFocusModes[0])
                    isAutoFocusEnabled = false
                }
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
            }
            result = isAutoFocusEnabled
        } catch (e: Exception) {
            isAutoFocusEnabled = false
            Log.e(TAG, "Error", e)
        }
        return result
    }

    fun disableAutoFocus(): Boolean {
        val result = false
        val characteristics = cameraCharacteristics ?: return false
        val builder = builderInputSurface ?: return false
        val session = cameraCaptureSession ?: return false
        val supportedFocusModes = characteristics.secureGet(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: return false
        for (mode in supportedFocusModes) {
            try {
                if (mode == CaptureRequest.CONTROL_AF_MODE_OFF) {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                    session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
                    isAutoFocusEnabled = false
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }
        return result
    }

    fun enableFaceDetection(faceDetectorCallback: FaceDetectorCallback?): Boolean {
        val characteristics = cameraCharacteristics ?: return false
        val builder = builderInputSurface ?: return false
        faceSensorScale = characteristics.secureGet(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        sensorOrientation = characteristics.secureGet(CameraCharacteristics.SENSOR_ORIENTATION) ?: return false
        val fd = characteristics.secureGet(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES) ?: return false
        val maxFD = characteristics.secureGet(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT) ?: return false
        if (fd.isEmpty() || maxFD <= 0) return false
        this.faceDetectorCallback = faceDetectorCallback
        faceDetectionEnabled = true
        faceDetectionMode = fd.toList().max()
        if (faceDetectionEnabled) {
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectionMode)
        }
        prepareFaceDetectionCallback()
        return true
    }

    fun disableFaceDetection() {
        if (faceDetectionEnabled) {
            faceDetectorCallback = null
            faceDetectionEnabled = false
            faceDetectionMode = 0
            prepareFaceDetectionCallback()
        }
    }

    fun isFaceDetectionEnabled() = faceDetectorCallback != null

    fun setCameraCallbacks(cameraCallbacks: CameraCallbacks?) {
        this.cameraCallbacks = cameraCallbacks
    }

    private fun prepareFaceDetectionCallback() {
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        try {
            session.stopRepeating()
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    private val cb: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            val faces = result.get(CaptureResult.STATISTICS_FACES) ?: return
            faceDetectorCallback?.onGetFaces(mapCamera2Faces(faces), faceSensorScale, sensorOrientation)
        }
    }

    fun openCameraId(cameraId: String) {
        this.cameraId = cameraId
        if (isPrepared) {
            val handlerThread = HandlerThread("$TAG Id = $cameraId")
            handlerThread.start()
            cameraHandler = Handler(handlerThread.looper)
            try {
                cameraManager.openCamera(cameraId, this, cameraHandler)
                semaphore.acquireUninterruptibly()
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                isRunning = true
                val face = characteristics.get(CameraCharacteristics.LENS_FACING)
                this.facing = if (face == CameraMetadata.LENS_FACING_FRONT) Facing.FRONT else Facing.BACK
                cameraCallbacks?.onCameraChanged(this.facing)
            } catch (e: Exception) {
                cameraCallbacks?.onCameraError("Open camera $cameraId failed")
                Log.e(TAG, "Error", e)
            }
        } else {
            throw IllegalStateException("You need to prepare the camera before opening it")
        }
    }

    val camerasAvailable: Array<String> = cameraManager.cameraIdList

    fun switchCamera() {
        try {
            val camId = if (cameraDevice == null || facing == Facing.FRONT) {
                getCameraIdForFacing(Facing.BACK)
            } else {
                getCameraIdForFacing(Facing.FRONT)
            }
            reOpenCamera(camId)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    fun reOpenCamera(cameraId: String) {
        if (cameraDevice != null) {
            closeCamera(false)
            prepareCamera(surfaceEncoder, fps)
            openCameraId(cameraId)
        }
    }

    val zoomRange: Range<Float>
        get() {
            val characteristics = cameraCharacteristics ?: return Range(1f, 1f)
            var zoomRanges: Range<Float>? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && levelSupported != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                zoomRanges = characteristics.secureGet(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            }
            if (zoomRanges == null) {
                val maxZoom = characteristics.secureGet(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
                zoomRanges = Range(1f, maxZoom)
            }
            return zoomRanges
        }

    var zoom: Float
        get() = zoomLevel
        set(level) {
            val characteristics = cameraCharacteristics ?: return
            val builder = builderInputSurface ?: return
            val session = cameraCaptureSession ?: return
            val l = level.coerceIn(zoomRange.lower, zoomRange.upper)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && levelSupported != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, l)
                } else {
                    val rect = characteristics.secureGet(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
                    val ratio = 1f / l
                    val croppedWidth = rect.width() - Math.round(rect.width().toFloat() * ratio)
                    val croppedHeight = rect.height() - Math.round(rect.height().toFloat() * ratio)
                    val cropRect = android.graphics.Rect(
                        croppedWidth / 2,
                        croppedHeight / 2,
                        rect.width() - croppedWidth / 2,
                        rect.height() - croppedHeight / 2
                    )
                    builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
                }
                session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
                zoomLevel = l
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
            }
        }

    fun getOpticalZooms(): Array<Float> {
        val characteristics = cameraCharacteristics ?: return arrayOf()
        return characteristics.secureGet(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toTypedArray() ?: arrayOf()
    }

    fun setOpticalZoom(level: Float) {
        val builder = builderInputSurface ?: return
        val session = cameraCaptureSession ?: return
        try {
            builder.set(CaptureRequest.LENS_FOCAL_LENGTH, level)
            session.setRepeatingRequest(builder.build(), if (faceDetectionEnabled) cb else null, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
        }
    }

    @JvmOverloads
    fun setZoom(event: MotionEvent, delta: Float = 0.1f) {
        if (event.pointerCount < 2 || event.action != MotionEvent.ACTION_MOVE) return
        val currentSpacing = CameraHelper.getFingerSpacing(event)
        if (currentSpacing > fingerSpacing) {
            zoom += delta
        } else if (currentSpacing < fingerSpacing) {
            zoom -= delta
        }
        fingerSpacing = currentSpacing
    }

    @JvmOverloads
    fun closeCamera(resetSurface: Boolean = true) {
        isLanternEnabled = false
        zoomLevel = 1.0f
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
        cameraHandler?.looper?.quitSafely()
        cameraHandler = null
        if (resetSurface) {
            surfaceEncoder = Surface(SurfaceTexture(-1).apply { release() })
            builderInputSurface = null
        }
        isPrepared = false
        isRunning = false
    }

    fun addImageListener(width: Int, height: Int, format: Int, maxImages: Int, autoClose: Boolean, listener: ImageCallback) {
        val wasRunning = isRunning
        closeCamera(false)
        this.imageReader?.close()
        val imageThread = HandlerThread("$TAG imageThread")
        imageThread.start()
        val imageReader = ImageReader.newInstance(width, height, format, maxImages)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                listener.onImageAvailable(image)
                if (autoClose) image.close()
            }
        }, Handler(imageThread.looper))
        this.imageReader = imageReader
        if (wasRunning) {
            prepareCamera(surfaceEncoder, fps)
            openLastCamera()
        }
    }

    fun removeImageListener() {
        val imageReader = this.imageReader ?: return
        val wasRunning = isRunning
        if (wasRunning) closeCamera(false)
        imageReader.close()
        this.imageReader = null
        if (wasRunning) {
            prepareCamera(surfaceEncoder, fps)
            openLastCamera()
        }
    }

    override fun onOpened(cameraDevice: CameraDevice) {
        this.cameraDevice = cameraDevice
        startPreview(cameraDevice)
        // Reapply any manual settings saved in the owner.
        owner?.reapplySettings()
        semaphore.release()
        cameraCallbacks?.onCameraOpened()
        Log.i(TAG, "Camera opened")
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
        cameraDevice.close()
        semaphore.release()
        cameraCallbacks?.onCameraDisconnected()
        Log.i(TAG, "Camera disconnected")
    }

    override fun onError(cameraDevice: CameraDevice, error: Int) {
        cameraDevice.close()
        semaphore.release()
        cameraCallbacks?.onCameraError("Open camera failed: $error")
        Log.e(TAG, "Open failed: $error")
    }

    @JvmOverloads
    fun getCameraIdForFacing(facing: Facing, cameraManager: CameraManager = this.cameraManager): String {
        val selectedFacing = if (facing == Facing.BACK) CameraMetadata.LENS_FACING_BACK else CameraMetadata.LENS_FACING_FRONT
        val ids = cameraManager.cameraIdList
        for (camId in ids) {
            val camFacing = cameraManager.getCameraCharacteristics(camId).get(CameraCharacteristics.LENS_FACING)
            if (camFacing != null && camFacing == selectedFacing) {
                return camId
            }
        }
        if (ids.isEmpty()) throw CameraOpenException("Camera not detected")
        return ids[0]
    }

    private fun getFacingByCameraId(cameraManager: CameraManager, cameraId: String): Facing {
        try {
            for (id in cameraManager.cameraIdList) {
                if (id == cameraId) {
                    val camFacing = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)
                    return if (camFacing == CameraMetadata.LENS_FACING_BACK) Facing.BACK else Facing.FRONT
                }
            }
            return Facing.BACK
        } catch (e: Exception) {
            return Facing.BACK
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSession(
        cameraDevice: CameraDevice,
        surfaces: List<Surface>,
        onConfigured: (CameraCaptureSession) -> Unit,
        onConfiguredFailed: (CameraCaptureSession) -> Unit,
        handler: Handler?
    ) {
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                onConfigured(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                onConfiguredFailed(session)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val config = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                surfaces.map { OutputConfiguration(it) },
                Executors.newSingleThreadExecutor(),
                callback
            )
            cameraDevice.createCaptureSession(config)
        } else {
            cameraDevice.createCaptureSession(surfaces, callback, handler)
        }
    }
}
