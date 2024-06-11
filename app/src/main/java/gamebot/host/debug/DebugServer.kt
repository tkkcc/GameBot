package gamebot.host.debug

import android.graphics.Bitmap
import gamebot.host.RemoteRun
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread


class DebugServer(val remoteRun: RemoteRun) {
    var serverThread: Thread? = null

    fun serve() {
        embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                // host SinglePageApplication
                get("/") {
                    call.respondText("ok")
                }

                get("/api/screenshot") {

                    ByteArrayOutputStream().apply {
                        val bitmap = remoteRun.refreshScreenshot()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
                    }.toByteArray().let {
                        call.respondBytes(it)
                    }


                }

                get("/api/screennode") {

                }
                post("/api/find") {

                }
                post("/api/ocr") {

                }
            }
        }.start(wait = true)
    }

    fun start() {
        serverThread = thread {
            serve()
        }
    }

    fun stop() {
        serverThread?.interrupt()
    }
}