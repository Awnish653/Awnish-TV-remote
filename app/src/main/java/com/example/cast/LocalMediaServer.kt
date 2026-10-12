package com.example.cast

import android.content.Context
import android.net.wifi.WifiManager
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.InetAddress
import java.util.UUID

/**
 * Serves the selected media file to a Cast receiver on the same Wi-Fi network.
 * The TV pulls the file; the content URI itself is never sent to the TV.
 */
class LocalMediaServer(private val mediaFile: File, private val mimeType: String, private val accessToken: String) :
    NanoHTTPD("0.0.0.0", PORT) {

    override fun serve(session: IHTTPSession): Response {
        if (session.uri != "/$accessToken") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
        if (session.method != Method.GET && session.method != Method.HEAD) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }
        if (!mediaFile.exists() || !mediaFile.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Media not found")
        }

        val total = mediaFile.length()
        if (total <= 0L) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Empty media file")
        }
        val rangeHeader = session.headers["range"]
        var start = 0L
        var end = total - 1
        var partial = false
        if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=")) {
            val range = rangeHeader.removePrefix("bytes=").substringBefore(",")
            val parts = range.split("-", limit = 2)
            val requestedStart = parts.getOrNull(0)?.toLongOrNull()
            val requestedEnd = parts.getOrNull(1)?.toLongOrNull()
            if (requestedStart == null && requestedEnd != null) {
                if (requestedEnd <= 0L) {
                    return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid byte range")
                        .apply { addHeader("Content-Range", "bytes */$total") }
                }
                start = (total - requestedEnd).coerceAtLeast(0L)
                end = total - 1
            } else {
                if (requestedStart != null) start = requestedStart
                if (requestedEnd != null) end = requestedEnd
            }
            if (start < 0L || start >= total || end < start) {
                return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid byte range")
                    .apply { addHeader("Content-Range", "bytes */$total") }
            }
            end = minOf(end, total - 1)
            partial = true
        }

        val length = end - start + 1
        val status = if (partial) Response.Status.PARTIAL_CONTENT else Response.Status.OK
        val response = if (session.method == Method.HEAD) {
            newFixedLengthResponse(status, mimeType, "")
        } else {
            val input = FileInputStream(mediaFile)
            var skipped = 0L
            while (skipped < start) {
                val count = input.skip(start - skipped)
                if (count <= 0) break
                skipped += count
            }
            if (skipped != start) {
                input.close()
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Could not seek media file")
            }
            newFixedLengthResponse(status, mimeType, input, length)
        }
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", length.toString())
        if (partial) response.addHeader("Content-Range", "bytes $start-$end/$total")
        response.addHeader("Cache-Control", "no-store")
        return response
    }

    companion object {
        private const val PORT = 18765

        fun create(context: Context, sourceFile: File, mimeType: String): Pair<LocalMediaServer, String> {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: error("Wi-Fi service is unavailable")
            val ip = wifi.connectionInfo?.ipAddress ?: 0
            if (ip == 0) error("Connect the phone to the same Wi-Fi as the TV")
            val address = InetAddress.getByAddress(
                byteArrayOf(
                    (ip and 0xff).toByte(),
                    (ip shr 8 and 0xff).toByte(),
                    (ip shr 16 and 0xff).toByte(),
                    (ip shr 24 and 0xff).toByte()
                )
            ).hostAddress ?: error("Could not determine phone Wi-Fi address")
            val token = UUID.randomUUID().toString().replace("-", "")
            val server = LocalMediaServer(sourceFile, mimeType, token)
            server.start(SOCKET_READ_TIMEOUT, false)
            return server to "http://$address:$PORT/$token"
        }
    }
}
