package com.messmate.bd.ui.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Creates the QR code that a member scans to join a mess.
 *
 * The payload is the deep link `messmate://join/<spreadsheetId>` – the
 * `JoinMessActivity` picks it up and opens the join flow.
 */
object QrCodeGenerator {

    /**
     * @param sizePx the size of the square bitmap.
     * @return the QR code or `null` when the content could not be encoded.
     */
    fun generate(content: String, sizePx: Int = 512): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints,
        )

        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                pixels[y * width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()

    /** The deep link of a mess. */
    fun joinLink(spreadsheetId: String): String = "messmate://join/$spreadsheetId"
}
