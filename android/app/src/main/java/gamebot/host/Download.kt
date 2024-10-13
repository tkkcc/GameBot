import android.os.SystemClock
import gamebot.host.d
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

fun downloadFile(
    url: String,
    path: String,
    scope: CoroutineScope,
    progressListener: ProgressListener? = null
): Deferred<Unit> {
    val client = HttpClient {
//        install(createClientPlugin("fix") {
//            on(Send) { request ->
//                d(request.body)
////                request.setBody(EmptyContent)
////                request.headers.remove("Accept-Charset")
//                this.proceed(request)
//            }
//        })
//        install(HttpCookies)
//        install(ContentEncoding)
//        install(Logging)
    }
    val file = File(path)
    file.delete()

    return scope.async(Dispatchers.IO) {
//        try {


            client.prepareGet(url).execute { httpResponse ->
    //            d(httpResponse.body<String>())
                val channel: ByteReadChannel = httpResponse.body()

    //            return@execute
                val total: Long = httpResponse.contentLength() ?: Long.MAX_VALUE
                var prev_time = SystemClock.uptimeMillis()
                var prev_byte = 0
                var byte = 0
    //            d("download 1")
                while (!channel.isClosedForRead) {
    //                d("download 2")

                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())

                    while (!packet.exhausted()) {
    //                    d("download 3")

                        val byteArray = packet.readByteArray()
    //                    d("download 4")

                        file.appendBytes(byteArray)
    //                    d("download 5")

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
    //                d("download 19")

                }
    //            d("download 20")

            }
//        } catch (e: CancellationException){
//            throw e
//        } catch (e: Throwable) {
//        }
    }
}