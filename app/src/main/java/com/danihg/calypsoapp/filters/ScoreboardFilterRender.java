package com.danihg.calypsoapp.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.danihg.calypsoapp.R;
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.utils.gl.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ScoreboardFilterRender extends BaseFilterRender {

    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "uniform sampler2D uSampler;\n" +
                    "uniform sampler2D uLeftLogoSampler;\n" +
                    "uniform sampler2D uRightLogoSampler;\n" +
                    "uniform int uScore;\n" +
                    "uniform vec4 uScoreColor;\n" +
                    "uniform vec2 uLeftLogoSize;\n" +
                    "uniform vec2 uRightLogoSize;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "\n" +
                    "float drawDigit(vec2 coord, int digit) {\n" +
                    "    vec2 pos = coord - vec2(0.5);\n" +
                    "    float d = length(pos);\n" +
                    "    \n" +
                    "    float thickness = 0.05;\n" +
                    "    float t1 = smoothstep(0.25, 0.2, d);\n" +
                    "    float t2 = smoothstep(0.25 + thickness, 0.25, d);\n" +
                    "    \n" +
                    "    float h = abs(pos.y) - 0.15;\n" +
                    "    float v = abs(pos.x) - 0.15;\n" +
                    "    \n" +
                    "    float horiz = smoothstep(thickness, 0.0, h);\n" +
                    "    float vert = smoothstep(thickness, 0.0, v);\n" +
                    "    \n" +
                    "    float top = step(pos.y, 0.15) * horiz;\n" +
                    "    float bot = step(-0.15, pos.y) * horiz;\n" +
                    "    float mid = step(-0.05, pos.y) * step(pos.y, 0.05) * horiz;\n" +
                    "    \n" +
                    "    float left = step(pos.x, 0.0) * vert;\n" +
                    "    float right = step(0.0, pos.x) * vert;\n" +
                    "    \n" +
                    "    float shape = 0.0;\n" +
                    "    if (digit == 0) shape = top + bot + left + right;\n" +
                    "    if (digit == 1) shape = right;\n" +
                    "    if (digit == 2) shape = top + mid + bot + step(0.0, pos.x) * vert + step(pos.x, 0.0) * step(-0.15, pos.y) * vert;\n" +
                    "    if (digit == 3) shape = top + mid + bot + right;\n" +
                    "    if (digit == 4) shape = mid + left * step(pos.y, 0.0) + right;\n" +
                    "    if (digit == 5) shape = top + mid + bot + step(pos.x, 0.0) * vert + step(0.0, pos.x) * step(pos.y, -0.15) * vert;\n" +
                    "    if (digit == 6) shape = top + mid + bot + left + step(0.0, pos.x) * step(pos.y, -0.15) * vert;\n" +
                    "    if (digit == 7) shape = top + right;\n" +
                    "    if (digit == 8) shape = top + mid + bot + left + right;\n" +
                    "    if (digit == 9) shape = top + mid + bot + left * step(pos.y, 0.0) + right;\n" +
                    "    \n" +
                    "    return min(shape, t1 - t2);\n" +
                    "}\n" +
                    "\n" +
                    "vec2 rotate(vec2 v, float angle) {\n" +
                    "    float cosTheta = cos(angle);\n" +
                    "    float sinTheta = sin(angle);\n" +
                    "    return vec2(\n" +
                    "            v.x * cosTheta - v.y * sinTheta,\n" +
                    "            v.x * sinTheta + v.y * cosTheta\n" +
                    "    );\n" +
                    "}\n" +
                    "\n" +
                    "float roundedBox(vec2 coord, vec2 size, float radius) {\n" +
                    "    vec2 q = abs(coord) - size + radius;\n" +
                    "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec4 texColor = texture2D(uSampler, vTextureCoord);\n" +
                    "    vec2 scoreboardPos = vec2(0.08, 0.05);  // Top-left corner\n" +
                    "    vec2 scoreboardSize = vec2(0.25, 0.1);\n" +
                    "    vec2 adjustedCoord = vec2(vTextureCoord.x, 1.0 - vTextureCoord.y);  // Flip Y-axis\n" +
                    "    vec2 localCoord = (adjustedCoord - scoreboardPos) / scoreboardSize;\n" +
                    "    \n" +
                    "    // Rotate 90 degrees left\n" +
                    "    localCoord = vec2(1.0 - localCoord.y, localCoord.x);\n" +
                    "    \n" +
                    "    float boxDist = roundedBox(localCoord - 0.5, vec2(0.5), 0.1);\n" +
                    "    \n" +
                    "    // Render logos\n" +
                    "    vec2 leftLogoPos = scoreboardPos + vec2(-0.095, 0.02);  // Left of the scoreboard\n" +
                    "    vec2 rightLogoPos = scoreboardPos + vec2(scoreboardSize.x - 0.02, 0.02);  // Right of the scoreboard\n" +
                    "    vec2 logoSize = vec2(0.06, 0.07 * uLeftLogoSize.y / uLeftLogoSize.x);\n" +
                    "    \n" +
                    "    vec2 leftLogoCoord = (adjustedCoord - leftLogoPos) / logoSize;\n" +
                    "    vec2 rightLogoCoord = (adjustedCoord - rightLogoPos) / logoSize;\n" +
                    "    \n" +
                    "    // Rotate logos 90 degrees over the x-axis\n" +
                    "    leftLogoCoord = vec2(0.5 - leftLogoCoord.x, 1.0 - leftLogoCoord.y);\n" +
                    "    rightLogoCoord = vec2(0.5 - rightLogoCoord.x, 1.0 - rightLogoCoord.y);\n" +
                    "    \n" +
                    "    // Rotate logo coordinates to match scoreboard orientation\n" +
                    "    leftLogoCoord = vec2(1.0 - leftLogoCoord.y, leftLogoCoord.x);\n" +
                    "    rightLogoCoord = vec2(1.0 - rightLogoCoord.y, rightLogoCoord.x);\n" +
                    "    leftLogoCoord = rotate(leftLogoCoord, radians(90.0));\n" +
                    "    rightLogoCoord = rotate(rightLogoCoord, radians(90.0));\n" +
                    "    \n" +
                    "    if (leftLogoCoord.x >= 0.0 && leftLogoCoord.x <= 1.0 && leftLogoCoord.y >= 0.0 && leftLogoCoord.y <= 1.0) {\n" +
                    "        vec4 leftLogoColor = texture2D(uLeftLogoSampler, leftLogoCoord);\n" +
                    "        texColor = mix(texColor, leftLogoColor, leftLogoColor.a);\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (rightLogoCoord.x >= 0.0 && rightLogoCoord.x <= 1.0 && rightLogoCoord.y >= 0.0 && rightLogoCoord.y <= 1.0) {\n" +
                    "        vec4 rightLogoColor = texture2D(uRightLogoSampler, rightLogoCoord);\n" +
                    "        texColor = mix(texColor, rightLogoColor, rightLogoColor.a);\n" +
                    "    }\n" +
                    "    \n" +
                    "    if (boxDist < 0.0) {\n" +
                    "        vec4 bgColor = mix(vec4(0.2, 0.2, 0.2, 0.9), vec4(0.3, 0.3, 0.3, 0.9), localCoord.y);\n" +
                    "        \n" +
                    "        int digit1 = uScore / 10;\n" +
                    "        int digit2 = int(mod(float(uScore), 10.0));\n" +
                    "        \n" +
                    "        float d1 = drawDigit((localCoord - vec2(0.3, 0.5)) * 2.0, digit1);\n" +
                    "        float d2 = drawDigit((localCoord - vec2(0.7, 0.5)) * 2.0, digit2);\n" +
                    "        \n" +
                    "        vec4 scoreColor = mix(bgColor, uScoreColor, max(d1, d2));\n" +
                    "        \n" +
                    "        float borderWidth = 0.01;\n" +
                    "        float borderSoftness = 0.002;\n" +
                    "        float borderAlpha = smoothstep(-borderSoftness, 0.0, boxDist + borderWidth) - smoothstep(0.0, borderSoftness, boxDist);\n" +
                    "        vec4 borderColor = vec4(1.0, 1.0, 1.0, 0.5);\n" +
                    "        \n" +
                    "        texColor = mix(scoreColor, borderColor, borderAlpha);\n" +
                    "    }\n" +
                    "    \n" +
                    "    gl_FragColor = texColor;\n" +
                    "}";


    private final float[] squareVertexDataFilter = {
            // X, Y, Z, U, V
            -1f, -1f, 0f, 0f, 0f, //bottom left
            1f, -1f, 0f, 1f, 0f, //bottom right
            -1f, 1f, 0f, 0f, 1f, //top left
            1f, 1f, 0f, 1f, 1f, //top right
    };

    private int program = -1;
    private int aPositionHandle = -1;
    private int aTextureHandle = -1;
    private int uMVPMatrixHandle = -1;
    private int uSTMatrixHandle = -1;
    private int uSamplerHandle = -1;
    private int uLeftLogoSamplerHandle = -1;
    private int uRightLogoSamplerHandle = -1;
    private int uScoreHandle = -1;
    private int uScoreColorHandle = -1;
    private int uLeftLogoSizeHandle = -1;
    private int uRightLogoSizeHandle = -1;

    private int score = 0;
    private float[] scoreColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // White by default
    private int leftLogoTextureId = -1;
    private int rightLogoTextureId = -1;
    private float[] leftLogoSize = new float[]{1.0f, 1.0f};
    private float[] rightLogoSize = new float[]{1.0f, 1.0f};

    private Bitmap leftLogoBitmap;
    private Bitmap rightLogoBitmap;

    public ScoreboardFilterRender(Bitmap leftLogoBitmap, Bitmap rightLogoBitmap) {
        squareVertex = ByteBuffer.allocateDirect(squareVertexDataFilter.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        squareVertex.put(squareVertexDataFilter).position(0);
        Matrix.setIdentityM(MVPMatrix, 0);
        Matrix.setIdentityM(STMatrix, 0);
        this.leftLogoBitmap = leftLogoBitmap;
        this.rightLogoBitmap = rightLogoBitmap;
    }

    @Override
    protected void initGlFilter(Context context) {
        program = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix");
        uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler");
        uLeftLogoSamplerHandle = GLES20.glGetUniformLocation(program, "uLeftLogoSampler");
        uRightLogoSamplerHandle = GLES20.glGetUniformLocation(program, "uRightLogoSampler");
        uScoreHandle = GLES20.glGetUniformLocation(program, "uScore");
        uScoreColorHandle = GLES20.glGetUniformLocation(program, "uScoreColor");
        uLeftLogoSizeHandle = GLES20.glGetUniformLocation(program, "uLeftLogoSize");
        uRightLogoSizeHandle = GLES20.glGetUniformLocation(program, "uRightLogoSize");

        // Load and set up the logo textures
        leftLogoTextureId = createLogoTexture(leftLogoBitmap, leftLogoSize);
        rightLogoTextureId = createLogoTexture(rightLogoBitmap, rightLogoSize);
    }

    private int createLogoTexture(Bitmap logoBitmap, float[] logoSize) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, logoBitmap, 0);

        // Generate mipmaps for better quality on downscaling
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // Calculate the aspect ratio of the logo
        float logoAspectRatio = (float) logoBitmap.getWidth() / (float) logoBitmap.getHeight();
        logoSize[0] = logoAspectRatio; // Width
        logoSize[1] = 1.0f; // Height

        return textures[0];
    }

    @Override
    protected void drawFilter() {
        GLES20.glUseProgram(program);

        squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
        GLES20.glEnableVertexAttribArray(aPositionHandle);

        squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex);
        GLES20.glEnableVertexAttribArray(aTextureHandle);

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0);

        GLES20.glUniform1i(uSamplerHandle, 4);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previousTexId);

        GLES20.glUniform1i(uLeftLogoSamplerHandle, 5);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, leftLogoTextureId);

        GLES20.glUniform1i(uRightLogoSamplerHandle, 6);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, rightLogoTextureId);

        GLES20.glUniform1i(uScoreHandle, score);
        GLES20.glUniform4fv(uScoreColorHandle, 1, scoreColor, 0);
        GLES20.glUniform2fv(uLeftLogoSizeHandle, 1, leftLogoSize, 0);
        GLES20.glUniform2fv(uRightLogoSizeHandle, 1, rightLogoSize, 0);
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(program);
        GLES20.glDeleteTextures(1, new int[]{leftLogoTextureId}, 0);
        GLES20.glDeleteTextures(1, new int[]{rightLogoTextureId}, 0);
        if (leftLogoBitmap != null && !leftLogoBitmap.isRecycled()) {
            leftLogoBitmap.recycle();
        }
        if (rightLogoBitmap != null && !rightLogoBitmap.isRecycled()) {
            rightLogoBitmap.recycle();
        }
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setScoreColor(int color) {
        this.scoreColor[0] = Color.red(color) / 255f;
        this.scoreColor[1] = Color.green(color) / 255f;
        this.scoreColor[2] = Color.blue(color) / 255f;
        this.scoreColor[3] = Color.alpha(color) / 255f;
    }
}