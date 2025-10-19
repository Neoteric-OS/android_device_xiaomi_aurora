/*
 * Copyright (C) 2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.edgesuppression

class SuppressionRect {
    private val list = IntArray(8)
    private var type: Int = 0
    private var position: Int = 0
    private var topLeftX: Int = 0
    private var topLeftY: Int = 0
    private var bottomRightX: Int = 0
    private var bottomRightY: Int = 0

    fun setValue(t: Int, p: Int, tx: Int, ty: Int, bx: Int, by: Int) {
        type = t
        position = p
        topLeftX = tx
        topLeftY = ty
        bottomRightX = bx
        bottomRightY = by
    }

    fun getList(): IntArray {
        list[0] = type
        list[1] = position
        list[2] = topLeftX
        list[3] = topLeftY
        list[4] = bottomRightX
        list[5] = bottomRightY
        list[6] = 0
        list[7] = 0
        return list
    }

    override fun toString(): String {
        return "SuppressionRect{list=${list.joinToString(prefix = \"[\", postfix = \"]\")}, type=$type, position=$position, topLeftX=$topLeftX, topLeftY=$topLeftY, bottomRightX=$bottomRightX, bottomRightY=$bottomRightY, time=0, node=0}"
    }
}
