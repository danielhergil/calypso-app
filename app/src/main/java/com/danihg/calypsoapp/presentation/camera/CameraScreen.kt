package com.danihg.calypsoapp.presentation.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.danihg.calypsoapp.R
import com.danihg.calypsoapp.filters.ScoreboardFilterRender
import com.danihg.calypsoapp.ui.theme.GreyTransparent
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.generic.GenericStream


@Composable
fun rememberToast(): (String) -> Unit {

    val context = LocalContext.current
    return remember { { message: String ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        // Lock orientation when the composable is active
        activity?.requestedOrientation = orientation

        onDispose {
            // Restore default orientation when leaving the composable
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

@SuppressLint("Wakelock", "WakelockTimeout")
@Composable
fun PreventScreenLock() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CameraScreen::WakeLock"
        )
        wakeLock.acquire()

        onDispose {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraScreen () {

    // Lock the screen to portrait mode
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    PreventScreenLock()

    val showToast = rememberToast()
    val context = LocalContext.current

    // Retrieve video/audio settings from SharedPreferences with default values
    val sharedPreferences = context.getSharedPreferences("CameraSettings", Context.MODE_PRIVATE)
    val videoWidth = sharedPreferences.getInt("videoWidth", 1920)
    val videoHeight = sharedPreferences.getInt("videoHeight", 1080)
    val videoBitrate = sharedPreferences.getInt("videoBitrate", 5000 * 1000)
    val videoFPS = sharedPreferences.getInt("videoFPS", 30)
    val audioSampleRate = sharedPreferences.getInt("audioSampleRate", 32000)
    val audioIsStereo = sharedPreferences.getBoolean("audioIsStereo", true)
    val audioBitrate = sharedPreferences.getInt("audioBitrate", 128 * 1000)


    // Scoreboard variables
    val options = BitmapFactory.Options()
    options.inScaled = false

    val leftLogoBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.rivas_50,
        options
    )
    val rightLogoBitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.alcorcon_50,
        options
    )
    val selectedBackgroundColor: Int = Color.Transparent.toArgb()
    var leftTeamGoals = 0
    val rightTeamGoals = 0
    val imageObjectFilterRender = ImageObjectFilterRender()

    var overlayDrawn by remember { mutableStateOf(false) }

    val genericStream = remember {
        GenericStream(context, object : ConnectChecker {
            override fun onConnectionStarted(url: String) {}
            override fun onConnectionSuccess() {
                showToast("Connected")
            }

            override fun onConnectionFailed(reason: String) {
                showToast("Connection failed: $reason")
            }

            override fun onNewBitrate(bitrate: Long) {}
            override fun onDisconnect() {
                showToast("Disconnected")
            }

            override fun onAuthError() {
                showToast("Authentication error")
            }

            override fun onAuthSuccess() {
                showToast("Authentication success")
            }
        }).apply {
            prepareVideo(videoWidth, videoHeight, videoBitrate, videoFPS)
            prepareAudio(audioSampleRate, audioIsStereo, audioBitrate)
            getGlInterface().autoHandleOrientation = true
//            setOrientation(180)
        }
    }

    var isStreaming by remember { mutableStateOf(false) }
    var isSettingsMenuVisible by remember { mutableStateOf(false) }

    fun createScoreboardBitmap(
        leftLogoBitmap: Bitmap?,
        rightLogoBitmap: Bitmap?,
        leftTeamGoals: Int,
        rightTeamGoals: Int,
        backgroundColor: Int
    ): Bitmap {
        val logoSize = 100
        val logoPadding = 20 // Padding between logos and scoreboard
        val scoreboardWidth = 500 // Reduced width of the scoreboard box
        val scoreboardHeight = 150
        val width = scoreboardWidth + 2 * logoSize + 4 * logoPadding // Extra space for logos and padding on both sides
        val height = scoreboardHeight
        val scoreboardLeft = logoSize + 2 * logoPadding.toFloat()
        val scoreboardRight = scoreboardLeft + scoreboardWidth
        val scoreboardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scoreboardBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = ResourcesCompat.getFont(context, R.font.montserrat_medium) // Ensure context is available here
        }

        // Draw scoreboard box
        paint.color = Color(0xFF222222).toArgb()
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(scoreboardLeft, 0f, scoreboardRight, height.toFloat(), 20f, 20f, paint)

        // Add a rounded rectangle border for the scoreboard
        paint.color = Color.White.toArgb()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawRoundRect(scoreboardLeft + 4f, 4f, scoreboardRight - 4f, height - 4f, 20f, 20f, paint)
        paint.style = Paint.Style.FILL // Reset paint style to fill

        // Draw team scores separated by a horizontal dash
        paint.color = Color.White.toArgb()
        paint.textSize = 80f
        paint.typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold) // Use a bolder font
        val scoreLeftX = scoreboardLeft + scoreboardWidth / 4f - 35f
        val scoreRightX = scoreboardLeft + 3 * scoreboardWidth / 4f - 35f
        val scoreCenterY = height / 2f + 30f
        canvas.drawText(leftTeamGoals.toString(), scoreLeftX, scoreCenterY, paint)
        canvas.drawText("-", (scoreboardLeft + scoreboardRight) / 2f - 20f, scoreCenterY, paint)
        canvas.drawText(rightTeamGoals.toString(), scoreRightX, scoreCenterY, paint)

        // Draw team logos on the sides of the scoreboard
        leftLogoBitmap?.let {
            val destRect1 = Rect(logoPadding, (height - logoSize) / 2, logoPadding + logoSize, (height + logoSize) / 2)
            canvas.drawBitmap(it, null, destRect1, null)
        }

        rightLogoBitmap?.let {
            val destRect2 = Rect(width - logoPadding - logoSize, (height - logoSize) / 2, width - logoPadding, (height + logoSize) / 2)
            canvas.drawBitmap(it, null, destRect2, null)
        }

        return scoreboardBitmap
    }

    fun updateOverlay() {
        Handler(Looper.getMainLooper()).post {
            val scoreboardBitmap: Bitmap = createScoreboardBitmap(
                leftLogoBitmap,
                rightLogoBitmap,
                leftTeamGoals,
                rightTeamGoals,
                selectedBackgroundColor
            )
            imageObjectFilterRender.setImage(scoreboardBitmap)


            val bitmapWidth = scoreboardBitmap.width
            val bitmapHeight = scoreboardBitmap.height
            val scaleX = 33.3f
            val scaleY = bitmapHeight.toFloat() / bitmapWidth.toFloat() * scaleX

            imageObjectFilterRender.setScale(scaleX, scaleY)

            // Calculate new position for left-right alignment with padding
            val paddingLeft = 3f
            val paddingTop = 3f
            imageObjectFilterRender.setPosition(paddingLeft, paddingTop)
        }
    }

    fun drawOverlay() {
        if (genericStream.isOnPreview) {
            updateOverlay()
            Log.d("CameraScreen", "Overlay drawn")
        } else {
            Log.d("CameraScreen", "Preview not active, overlay not drawn")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Fills the entire screen
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Removes insets added by Scaffold
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                // SurfaceView for camera preview

//                fun createScoreboardBitmap(
//                    leftLogoBitmap: Bitmap?,
//                    rightLogoBitmap: Bitmap?,
//                    leftTeamGoals: Int,
//                    rightTeamGoals: Int,
//                    backgroundColor: Int
//                ): Bitmap {
//                    val logoSize = 100
//                    val logoPadding = 20 // Padding between logos and scoreboard
//                    val scoreboardWidth = 500 // Reduced width of the scoreboard box
//                    val scoreboardHeight = 150
//                    val width = scoreboardWidth + 2 * logoSize + 4 * logoPadding // Extra space for logos and padding on both sides
//                    val height = scoreboardHeight
//                    val scoreboardLeft = logoSize + 2 * logoPadding.toFloat()
//                    val scoreboardRight = scoreboardLeft + scoreboardWidth
//                    val scoreboardBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//                    val canvas = Canvas(scoreboardBitmap)
//                    val paint = Paint().apply {
//                        isAntiAlias = true
//                        typeface = ResourcesCompat.getFont(context, R.font.montserrat_medium) // Ensure context is available here
//                    }
//
//                    // Draw scoreboard box
//                    paint.color = Color(0xFF222222).toArgb()
//                    paint.style = Paint.Style.FILL
//                    canvas.drawRoundRect(scoreboardLeft, 0f, scoreboardRight, height.toFloat(), 20f, 20f, paint)
//
//                    // Add a rounded rectangle border for the scoreboard
//                    paint.color = Color.White.toArgb()
//                    paint.style = Paint.Style.STROKE
//                    paint.strokeWidth = 8f
//                    canvas.drawRoundRect(scoreboardLeft + 4f, 4f, scoreboardRight - 4f, height - 4f, 20f, 20f, paint)
//                    paint.style = Paint.Style.FILL // Reset paint style to fill
//
//                    // Draw team scores separated by a horizontal dash
//                    paint.color = Color.White.toArgb()
//                    paint.textSize = 80f
//                    paint.typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold) // Use a bolder font
//                    val scoreLeftX = scoreboardLeft + scoreboardWidth / 4f - 35f
//                    val scoreRightX = scoreboardLeft + 3 * scoreboardWidth / 4f - 35f
//                    val scoreCenterY = height / 2f + 30f
//                    canvas.drawText(leftTeamGoals.toString(), scoreLeftX, scoreCenterY, paint)
//                    canvas.drawText("-", (scoreboardLeft + scoreboardRight) / 2f - 20f, scoreCenterY, paint)
//                    canvas.drawText(rightTeamGoals.toString(), scoreRightX, scoreCenterY, paint)
//
//                    // Draw team logos on the sides of the scoreboard
//                    leftLogoBitmap?.let {
//                        val destRect1 = Rect(logoPadding, (height - logoSize) / 2, logoPadding + logoSize, (height + logoSize) / 2)
//                        canvas.drawBitmap(it, null, destRect1, null)
//                    }
//
//                    rightLogoBitmap?.let {
//                        val destRect2 = Rect(width - logoPadding - logoSize, (height - logoSize) / 2, width - logoPadding, (height + logoSize) / 2)
//                        canvas.drawBitmap(it, null, destRect2, null)
//                    }
//
//                    return scoreboardBitmap
//                }
//
//                fun updateOverlay() {
//                    Handler(Looper.getMainLooper()).post {
//                        val scoreboardBitmap: Bitmap = createScoreboardBitmap(
//                            leftLogoBitmap,
//                            rightLogoBitmap,
//                            leftTeamGoals,
//                            rightTeamGoals,
//                            selectedBackgroundColor
//                        )
//                        imageObjectFilterRender.setImage(scoreboardBitmap)
//
//
//                        val bitmapWidth = scoreboardBitmap.width
//                        val bitmapHeight = scoreboardBitmap.height
//                        val scaleX = 33.3f
//                        val scaleY = bitmapHeight.toFloat() / bitmapWidth.toFloat() * scaleX
//
//                        imageObjectFilterRender.setScale(scaleX, scaleY)
//
//                        // Calculate new position for left-right alignment with padding
//                        val paddingLeft = 3f
//                        val paddingTop = 3f
//                        imageObjectFilterRender.setPosition(paddingLeft, paddingTop)
//                    }
//                }

//                fun drawOverlay() {
//                    if (genericStream.isOnPreview) {
//                        updateOverlay()
//                        Log.d("CameraScreen", "Overlay drawn")
//                    } else {
//                        Log.d("CameraScreen", "Preview not active, overlay not drawn")
//                    }
//                }

                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
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
                    modifier = Modifier.fillMaxSize() // Fills the screen
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(160.dp)
                        .padding(end = 80.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top button (Left Button in previous version)
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_rocket),
                            onClick = {
                                genericStream.getGlInterface().addFilter(imageObjectFilterRender)
                                drawOverlay()
                            }
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Red circular button (Center Button)
                        Button(
                            onClick = {
                                if (isStreaming) {
                                    genericStream.stopStream()
                                    isStreaming = false
                                } else {
                                    genericStream.startStream("rtmp://a.rtmp.youtube.com/live2/j2sh-690b-fg9y-2fah-7444")
                                    isStreaming = true
                                }
                            },
                            modifier = Modifier
                                .size(70.dp)
                                .border(3.dp, Color.White, CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            ),
                            shape = CircleShape
                        ) {}

                        Spacer(modifier = Modifier.height(5.dp))

                        // Bottom button (Right Button in previous version)
                        AuxButton(
                            modifier = Modifier
                                .size(50.dp)
                                .zIndex(2f),
                            painter = painterResource(id = R.drawable.ic_settings),
                            onClick = {
                                isSettingsMenuVisible = !isSettingsMenuVisible
                            }
                        )
                    }
                }

                // Fullscreen Settings Menu
                AnimatedVisibility(
                    visible = isSettingsMenuVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(tween(500)) + slideOutVertically(targetOffsetY = { it })
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ){
                            AuxButton(
                                modifier = Modifier
                                    .size(50.dp)
                                    .zIndex(2f),
                                painter = painterResource(id = R.drawable.ic_settings),
                                onClick = {
                                    leftTeamGoals++
                                    drawOverlay()
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
fun AuxButton(modifier: Modifier = Modifier, painter: Painter, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(GreyTransparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter,
            contentDescription = "Right Button Icon",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}

