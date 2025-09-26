package com.example.eswproject

import android.content.Context
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

data class Detection(val box: RectF, val score: Float, val classId: Int)

class PoseEngine(private val context: Context) {
    // File names in assets (place actual models there)
    private val yoloModelFile = "yolo_nas_person.tflite" // placeholder
    private val hrnetModelFile = "hrnet_pose.tflite"     // placeholder

    private var yolo: Interpreter? = null
    private var hrnet: Interpreter? = null

    fun load() {
        if (yolo == null) {
            try {
                val model = FileUtil.loadMappedFile(context, yoloModelFile)
                yolo = Interpreter(model)
            } catch (_: Throwable) { /* YOLO optional for now */ }
        }
        if (hrnet == null) {
            val model = FileUtil.loadMappedFile(context, hrnetModelFile)
            hrnet = Interpreter(model)
        }
    }

    fun isReady(): Boolean = hrnet != null

    // Simple person detector; returns whole image if YOLO not loaded
    fun detectPerson(inputW: Int, inputH: Int): RectF = RectF(0f, 0f, inputW.toFloat(), inputH.toFloat())

    // Run HRNet on cropped person region using NV21 frame; return 17 COCO keypoints in image coords
    fun runHrnet(nv21: ByteArray, imageWidth: Int, imageHeight: Int, roi: RectF): List<Triple<Float, Float, Float>> {
        val interpreter = hrnet ?: return emptyList()

        // HRNet common input is 256x192 (h,w) or 384x288; adjust if your model differs
        val inH = 256
        val inW = 192

        // Prepare input buffer (RGB float32 normalized 0..1) with ROI crop+resize from NV21
        val input = ByteBuffer.allocateDirect(inH * inW * 3 * 4).order(ByteOrder.nativeOrder())
        nv21CropResizeToRgbFloat(nv21, imageWidth, imageHeight, roi, inW, inH, input)

        // Output heatmaps: [1, inH/4, inW/4, 17]
        val outH = inH / 4
        val outW = inW / 4
        val numKp = 17
        val output = Array(1) { Array(outH) { Array(outW) { FloatArray(numKp) } } }

        interpreter.run(input, output)

        // Argmax per keypoint channel to get (u,v)
        val keypoints = mutableListOf<Triple<Float, Float, Float>>()
        for (k in 0 until numKp) {
            var best = -1f
            var bu = 0
            var bv = 0
            for (v in 0 until outH) {
                for (u in 0 until outW) {
                    val s = output[0][v][u][k]
                    if (s > best) { best = s; bu = u; bv = v }
                }
            }
            // Map back to input crop space then to image coords
            val xCrop = (bu + 0.5f) * (inW / outW.toFloat())
            val yCrop = (bv + 0.5f) * (inH / outH.toFloat())
            val xImg = roi.left + xCrop * (roi.width() / inW)
            val yImg = roi.top + yCrop * (roi.height() / inH)
            keypoints.add(Triple(xImg, yImg, max(0f, min(1f, best))))
        }
        return keypoints
    }

    private fun nv21CropResizeToRgbFloat(
        nv21: ByteArray,
        srcW: Int,
        srcH: Int,
        roi: RectF,
        dstW: Int,
        dstH: Int,
        out: ByteBuffer
    ) {
        out.rewind()
        val yPlaneSize = srcW * srcH
        // NV21 has VU pairs after Y plane
        val uvStart = yPlaneSize

        // Clamp ROI to image bounds
        val left = max(0f, min(srcW - 1f, roi.left))
        val top = max(0f, min(srcH - 1f, roi.top))
        val right = max(left + 1f, min(srcW.toFloat(), roi.right))
        val bottom = max(top + 1f, min(srcH.toFloat(), roi.bottom))

        val roiW = right - left
        val roiH = bottom - top

        for (j in 0 until dstH) {
            val srcYf = top + (j + 0.5f) * (roiH / dstH)
            val sy = min(srcH - 1, max(0, srcYf.toInt()))
            val chy = sy / 2
            for (i in 0 until dstW) {
                val srcXf = left + (i + 0.5f) * (roiW / dstW)
                val sx = min(srcW - 1, max(0, srcXf.toInt()))
                val chx = sx / 2

                val y = (nv21[sy * srcW + sx].toInt() and 0xFF)
                val vuIndex = uvStart + (chy * srcW) + (chx * 2)
                val v = (nv21[vuIndex].toInt() and 0xFF)
                val u = (nv21[vuIndex + 1].toInt() and 0xFF)

                // YUV to RGB (BT.601 full-range approximation)
                val Y = y.toFloat()
                val U = u - 128f
                val V = v - 128f
                var r = Y + 1.402f * V
                var g = Y - 0.344136f * U - 0.714136f * V
                var b = Y + 1.772f * U

                r = (r / 255f).coerceIn(0f, 1f)
                g = (g / 255f).coerceIn(0f, 1f)
                b = (b / 255f).coerceIn(0f, 1f)

                out.putFloat(r)
                out.putFloat(g)
                out.putFloat(b)
            }
        }
        out.rewind()
    }
}
