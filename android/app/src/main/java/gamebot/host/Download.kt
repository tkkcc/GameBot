import android.os.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.IOException
import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

fun checkSHA256(filePath: String, expectedSha256: String): Boolean {
    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(filePath).use { fis ->
            DigestInputStream(fis, digest).use { dis ->
                val buffer = ByteArray(8192)
                while (dis.read(buffer) != -1) {
                    // Reading data updates the digest automatically
                }
            }
        }

        val calculatedHash = digest.digest().toHexString()
        calculatedHash == expectedSha256
    } catch (e: Exception) {
        // Handle exceptions (e.g., log, rethrow, return false)
        // Log.e("SHA256Check", "Error calculating SHA-256", e) // Example logging
        false
    }
}

fun ByteArray.toHexString(): String {
    return joinToString("") { String.format("%02x", it) }
}

fun downloadFile2(
    url: String,
    path: String,
    scope: CoroutineScope,
    progressListener: ProgressListener? = null
): Deferred<Unit> {
    val request = Request.Builder()
        .url(url)
        .build()

    val client = OkHttpClient.Builder()
        .build()

    suspend fun writeResponseBodyToStorage(body: ResponseBody, path: File) =
        withContext(Dispatchers.IO) {

            val total = body.contentLength()
            var byte: Long = 0
            var prev_byte: Long = 0
            var prev_time = SystemClock.uptimeMillis()

            body.byteStream().use { inputStream ->
                path.outputStream().use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    while (true) {
                        val byteRead = inputStream.read(buffer)

                        if (byteRead < 0) {
                            break
                        }
                        outputStream.write(buffer, 0, byteRead)
                        byte += byteRead
                        progressListener?.let {
                            val time = SystemClock.uptimeMillis()

                            if (time - prev_time < 1000) {
                                return@let
                            }
                            progressListener.onUpdate(
                                byte.toFloat() / total.toFloat(),
                                (byte - prev_byte).toFloat() * 1000 / (time - prev_time).toFloat()
                            )
                            prev_byte = byte
                            prev_time = time
                        }
                    }
                    outputStream.flush()
                }
            }
        }


    return scope.async(Dispatchers.IO) {
        client.newCall(request).execute().use {
            if (it.isSuccessful) {
                writeResponseBodyToStorage(it.body!!, File(path))
            } else
                throw IOException("unexpected code " + it)
        }
    }

}

fun downloadFile(
    url: String,
    path: String,
    scope: CoroutineScope,
    progressListener: ProgressListener? = null
): Deferred<Unit> = scope.async(Dispatchers.IO) {
    val client = HttpClient()
    File(path).outputStream().use { file ->
        val buffer = ByteArray(4 * 1024)
        client.prepareGet(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            val total: Long = httpResponse.contentLength() ?: Long.MAX_VALUE
            var prev_time = SystemClock.uptimeMillis()
            var prev_byte = 0
            var byte = 0
            var bytePerSecond = 0f
            while (true) {
                val size = channel.readAvailable(buffer)
                if (size < 0) {
                    break
                }
                file.write(buffer, 0, size)
                byte += size;
                val time = SystemClock.uptimeMillis()
                progressListener?.let {
                    if (time - prev_time > 1000) {
                        bytePerSecond =
                            (byte - prev_byte).toFloat() * 1000 / (time - prev_time).toFloat()
                        prev_byte = byte
                        prev_time = time
                    }
                    progressListener.onUpdate(
                        byte.toFloat() / total.toFloat(),
                        bytePerSecond
                    )
                }
            }
        }
    }
}
