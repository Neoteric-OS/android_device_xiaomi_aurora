/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression

import android.content.Context
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.xiaomi.settings.R
import com.xiaomi.settings.touch.TfWrapper
import kotlin.math.max
import kotlin.math.min

class EdgeSuppressionManager private constructor(context: Context) {

    private val appContext: Context = context.applicationContext

    private var sendArray: IntArray
    private val rectArray: Array<SuppressionRect>
    private var index = 0
    private val screenHeight: Int
    private val screenWidth: Int
    @Suppress("unused")
    private val absoluteLevel: IntArray
    private val corner: IntArray

    private enum class Mode(val index: Int) {
        CORNER(0),
        CONDITION(1),
        ABSOLUTE(2);
    }

    init {
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        screenWidth = min(metrics.widthPixels, metrics.heightPixels) - 1
        screenHeight = max(metrics.widthPixels, metrics.heightPixels) - 1

        absoluteLevel = appContext.resources.getIntArray(R.array.edge_suppresson_absolute)
        corner = appContext.resources.getIntArray(R.array.edge_suppresson_corner)
        val rectSize = appContext.resources.getInteger(R.integer.edge_suppresson_rect_size)
        val sendSize = appContext.resources.getInteger(R.integer.edge_suppresson_send_size)
        rectArray = Array(rectSize) { SuppressionRect() }
        sendArray = IntArray(sendSize)
    }

    fun handleEdgeSuppressionChange(): IntArray {
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowManager.defaultDisplay.rotation
        val width = PreferenceManager.getDefaultSharedPreferences(appContext)
            .getFloat("edgesuppression_width_value", 0.8f)
        val suppressionRect = getSuppressionRect(rotation, width)
        TfWrapper.setTouchFeature(TfWrapper.TfParams(15, suppressionRect))
        return suppressionRect
    }

    private fun getSuppressionRect(rotation: Int, width: Float): IntArray {
        resetSendArray()
        if (rotation == 1 || rotation == 3) {
            setRectPointForHorizontal(getSuppressionSize(true, width), Mode.ABSOLUTE.index)
            setRectPointForHorizontal(getSuppressionSize(false, width), Mode.CONDITION.index)
        } else {
            setRectPointForPortrait(getSuppressionSize(true, width), Mode.ABSOLUTE.index)
            setRectPointForPortrait(getSuppressionSize(false, width), Mode.CONDITION.index)
        }
        setCornerRectPoint(rotation)
        compileSendArray()
        return sendArray
    }

    private fun setRectPointForHorizontal(width: Int, modeIndex: Int) {
        setRectValue(getCurrentRect(), modeIndex, 0, 0, 0, screenWidth, width)
        setRectValue(
            getCurrentRect(),
            modeIndex,
            1,
            0,
            screenHeight - width,
            screenWidth,
            screenHeight
        )
        setRectValue(getCurrentRect(), modeIndex, 2, 0, 0, width, screenHeight)
        setRectValue(
            getCurrentRect(),
            modeIndex,
            3,
            screenWidth - width,
            0,
            screenWidth,
            screenHeight
        )
    }

    private fun setRectPointForPortrait(width: Int, modeIndex: Int) {
        setRectValue(getCurrentRect(), modeIndex, 0, 0, 0, 0, 0)
        setRectValue(getCurrentRect(), modeIndex, 1, 0, 0, 0, 0)
        setRectValue(getCurrentRect(), modeIndex, 2, 0, 0, width, screenHeight)
        setRectValue(getCurrentRect(), modeIndex, 3, screenWidth - width, 0, screenWidth, screenHeight)
    }

    private fun setCornerRectPoint(rotation: Int) {
        when (rotation) {
            0 -> {
                setRectValue(getCurrentRect(), Mode.CORNER.index, 0, 0, 0, 0, 0)
                setRectValue(getCurrentRect(), Mode.CORNER.index, 1, 0, 0, 0, 0)
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    2,
                    0,
                    screenHeight - corner[1],
                    corner[0],
                    screenHeight
                )
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    3,
                    screenWidth - corner[0],
                    screenHeight - corner[1],
                    screenWidth,
                    screenHeight
                )
            }

            1 -> {
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    0,
                    0,
                    0,
                    corner[2],
                    corner[3]
                )
                setRectValue(getCurrentRect(), Mode.CORNER.index, 1, 0, 0, 0, 0)
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    2,
                    0,
                    screenHeight - corner[3],
                    corner[2],
                    screenHeight
                )
                setRectValue(getCurrentRect(), Mode.CORNER.index, 3, 0, 0, 0, 0)
            }

            2 -> {
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    0,
                    0,
                    0,
                    corner[0],
                    corner[1]
                )
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    1,
                    screenWidth - corner[0],
                    0,
                    screenWidth,
                    corner[1]
                )
                setRectValue(getCurrentRect(), Mode.CORNER.index, 2, 0, 0, 0, 0)
                setRectValue(getCurrentRect(), Mode.CORNER.index, 3, 0, 0, 0, 0)
            }

            3 -> {
                setRectValue(getCurrentRect(), Mode.CORNER.index, 0, 0, 0, 0, 0)
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    1,
                    screenWidth - corner[2],
                    0,
                    screenWidth,
                    corner[3]
                )
                setRectValue(getCurrentRect(), Mode.CORNER.index, 2, 0, 0, 0, 0)
                setRectValue(
                    getCurrentRect(),
                    Mode.CORNER.index,
                    3,
                    screenWidth - corner[2],
                    screenHeight - corner[3],
                    screenWidth,
                    screenHeight
                )
            }
        }
    }

    fun getSuppressionSize(absolute: Boolean, width: Float): Int {
        return (width * if (absolute) 10 else 50).toInt()
    }

    private fun setRectValue(
        suppressionRect: SuppressionRect,
        t: Int,
        p: Int,
        tx: Int,
        ty: Int,
        bx: Int,
        by: Int
    ) {
        suppressionRect.setValue(t, p, tx, ty, bx, by)
    }

    private fun compileSendArray() {
        var sendIndex = 0
        for (rect in rectArray) {
            val rectValues = rect.getList()
            System.arraycopy(rectValues, 0, sendArray, sendIndex, rectValues.size)
            sendIndex += rectValues.size
        }
        resetIndex()
    }

    private fun resetIndex() {
        index = 0
    }

    private fun getCurrentRect(): SuppressionRect {
        return rectArray[index++]
    }

    private fun resetSendArray() {
        sendArray.fill(0)
    }

    companion object {
        private const val TAG = "XiaomiPartsEdgeSuppressionManager"
        @Suppress("unused")
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

        @Volatile
        private var instance: EdgeSuppressionManager? = null

        fun getInstance(context: Context): EdgeSuppressionManager {
            return instance ?: synchronized(this) {
                instance ?: EdgeSuppressionManager(context).also { instance = it }
            }
        }
    }
}
