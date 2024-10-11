import android.os.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
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

fun downloadFile(url: String, path: String, scope: CoroutineScope, progressListener: ProgressListener? = null): Job {
    val client = HttpClient{
        install(HttpCookies)
        install(ContentEncoding)

        install(Logging)
    }
    val file = File(path)
    file.delete()

    return scope.launch(Dispatchers.IO) {
        client.prepareGet(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            val total: Long = httpResponse.contentLength() ?: Long.MAX_VALUE
            var prev_time = SystemClock.uptimeMillis()
            var prev_byte = 0
            var byte = 0
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.exhausted()) {
                    val byteArray = packet.readByteArray()
                    file.appendBytes(byteArray)

                    byte += byteArray.size
                    val time = SystemClock.uptimeMillis()
                    if (progressListener != null && (time - prev_time) > 1000) {
                        progressListener.onUpdate(
                            byte.toFloat() / total.toFloat(),
                            (byte - prev_byte).toFloat() * 1000 / (time - prev_time).toFloat()
                        )

                        prev_byte = byte
                        prev_time = time
                    }

                }
            }
        }
    }
}