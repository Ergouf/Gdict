package io.github.gdict.api

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

object AfdianClient {

    const val USER_ID = "7e6e7fd45a9e11f1ad7d52540025c377"

    private const val TOKEN = "mtH3gUXv7s8MekFY6qnASh9xpNPyrafj"

    const val BASE_URL = "https://ifdian.net/api/open"

    const val AFDIAN_SPONSOR_URL = "https://ifdian.net/a/fengjl"

    fun sign(params: String, ts: Long): String {
        val signStr = "${TOKEN}params${params}ts${ts}user_id${USER_ID}"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(signStr.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun request(endpoint: String, paramsJson: String): String? {
        return try {
            val ts = System.currentTimeMillis() / 1000
            val signStr = sign(paramsJson, ts)
            val body = buildJsonBody(paramsJson, ts, signStr)

            val url = URL("$BASE_URL/$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            conn.outputStream.use { os ->
                os.write(body.toByteArray())
                os.flush()
            }

            if (conn.responseCode in 200..299) {
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } else {
                conn.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildJsonBody(paramsJson: String, ts: Long, sign: String): String {
        val escapedParams = paramsJson
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return "{\"user_id\":\"$USER_ID\",\"params\":\"$escapedParams\",\"ts\":$ts,\"sign\":\"$sign\"}"
    }

    fun ping(): String? {
        return request("ping", """{"a":"1"}""")
    }

    fun queryOrder(page: Int = 1, perPage: Int = 50): String? {
        return request("query-order", """{"page":$page,"per_page":$perPage}""")
    }

    fun querySponsor(page: Int = 1, perPage: Int = 20): String? {
        return request("query-sponsor", """{"page":$page,"per_page":$perPage}""")
    }

    fun queryPlan(planId: String): String? {
        return request("query-plan", """{"plan_id":"$planId"}""")
    }

    fun openSponsorPage(amount: Int = 0): Boolean {
        return try {
            val url = if (amount > 0) {
                "$AFDIAN_SPONSOR_URL?custom_price=$amount"
            } else {
                AFDIAN_SPONSOR_URL
            }
            val desktop = java.awt.Desktop.getDesktop()
            desktop.browse(URI(url))
            true
        } catch (_: Exception) {
            false
        }
    }
}
