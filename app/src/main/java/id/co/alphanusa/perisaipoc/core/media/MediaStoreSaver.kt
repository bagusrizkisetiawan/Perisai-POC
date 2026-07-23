package id.co.alphanusa.perisaipoc.core.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menyimpan hasil rekaman & foto ke galeri publik agar mudah ditemukan.
 * - Android 10+ (Q): memakai MediaStore (scoped storage), tanpa izin storage.
 * - Android 9 ke bawah: menulis ke folder publik lalu memindai MediaScanner.
 *
 * Kelas ini murni data/IO — tidak menyentuh UI. Pemanggil yang menampilkan
 * pesan sukses/gagal berdasarkan [Result].
 */
class MediaStoreSaver(private val context: Context) {

    companion object {
        const val VIDEO_ALBUM = "PERISAI POC VIDEO"
        const val PHOTO_ALBUM = "PERISAI POC Photo"
    }

    /**
     * Memindahkan file rekaman sementara ke galeri (album [VIDEO_ALBUM]).
     * File sumber dihapus setelah berhasil disalin.
     */
    suspend fun saveVideoToGallery(source: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!source.exists()) error("File rekaman tidak ditemukan")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/" + VIDEO_ALBUM,
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Video.Media
                    .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                    ?: error("Gagal membuat entri galeri")
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                source.delete()
            } else {
                val moviesDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val albumDir = File(moviesDir, VIDEO_ALBUM)
                if (!albumDir.exists()) albumDir.mkdirs()
                val dest = File(albumDir, source.name)
                source.inputStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                source.delete()
                scan(dest, "video/mp4")
            }
            Unit
        }
    }

    /**
     * Menyimpan bitmap sebagai JPEG ke galeri (album [PHOTO_ALBUM]).
     */
    suspend fun savePhotoToGallery(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "IMG_${timestamp()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/" + PHOTO_ALBUM,
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Images.Media
                    .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                    ?: error("Gagal membuat entri galeri")
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                val picturesDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val albumDir = File(picturesDir, PHOTO_ALBUM)
                if (!albumDir.exists()) albumDir.mkdirs()
                val dest = File(albumDir, fileName)
                dest.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                scan(dest, "image/jpeg")
            }
            Unit
        }
    }

    /** Membuat nama file MP4 baru di folder khusus aplikasi (sumber sementara). */
    fun newRecordingFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (dir != null && !dir.exists()) dir.mkdirs()
        return File(dir, "REC_${timestamp()}.mp4")
    }

    private fun scan(file: File, mime: String) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mime),
            null,
        )
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}
