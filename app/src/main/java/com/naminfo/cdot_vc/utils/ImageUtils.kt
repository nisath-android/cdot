package com.naminfo.cdot_vc.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Base64
import java.io.FileNotFoundException
import com.naminfo.cdot_vc.compatibility.Compatibility
import org.linphone.core.tools.Log

class ImageUtils {
    companion object {
        private const val BASE64_REGEX = "^data:image\\/(gif|png|jpeg|bmp|webp|svg\\+xml)(;charset=utf-8)?;base64,[A-Za-z0-9+\\/]+={0,2}\$"

        fun isBase64(source: String): Boolean {
            return source.matches(Regex(BASE64_REGEX))
        }

        fun getBase64ImageFromString(base64: String): ByteArray? {
            val substring = base64.substring(base64.indexOf(",") + 1)
            return Base64.decode(substring, Base64.DEFAULT)
        }

        fun getRoundBitmapFromUri(
            context: Context,
            fromPictureUri: Uri?
        ): Bitmap? {
            var bm: Bitmap? = null
            if (fromPictureUri != null) {
                bm = try {
                    // We make a copy to ensure Bitmap will be Software and not Hardware, required for shortcuts
                    Compatibility.getBitmapFromUri(context, fromPictureUri).copy(
                        Bitmap.Config.ARGB_8888,
                        true
                    )
                } catch (fnfe: FileNotFoundException) {
                    return null
                } catch (e: Exception) {
                    Log.e("[Image Utils] Failed to get bitmap from URI [$fromPictureUri]: $e")
                    return null
                }
            }
            if (bm != null) {
                val roundBm = getRoundBitmap(bm)
                if (roundBm != null) {
                    bm.recycle()
                    return roundBm
                }
            }
            return bm
        }

        fun rotateImage(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            val rotatedBitmap = Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
            source.recycle()
            return rotatedBitmap
        }

        private fun getRoundBitmap(bitmap: Bitmap): Bitmap? {
            val output =
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect =
                Rect(0, 0, bitmap.width, bitmap.height)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawCircle(
                bitmap.width / 2.toFloat(),
                bitmap.height / 2.toFloat(),
                bitmap.width / 2.toFloat(),
                paint
            )
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
