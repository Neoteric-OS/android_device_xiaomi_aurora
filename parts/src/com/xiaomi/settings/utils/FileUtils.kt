/*
 * Copyright (C) 2022-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings.utils

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object FileUtils {
    private const val TAG = "XiaomiPartsFileUtils"
    private const val DEBUG = true

    @JvmStatic
    fun readLine(fileName: String): String? {
        var line: String? = null
        try {
            BufferedReader(FileReader(fileName), /*sz*/512).use { reader ->
                line = reader.readLine()
            }
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for reading", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file $fileName", e)
        }
        return line
    }

    @JvmStatic
    fun readLineInt(fileName: String): Int {
        val line = readLine(fileName)
        if (line == null) {
            Log.e(TAG, "readLineInt: line is null for file $fileName")
            return 0
        }
        return try {
            line.replace("0x", "").toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Could not convert string to int from file $fileName", e)
            0
        }
    }

    @JvmStatic
    fun writeLine(fileName: String, value: String) {
        try {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                writer.write(value)
            }
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for writing", e)
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file $fileName", e)
        }
    }

    @JvmStatic
    fun writeLine(fileName: String, value: Int) {
        writeLine(fileName, value.toString())
    }
}
