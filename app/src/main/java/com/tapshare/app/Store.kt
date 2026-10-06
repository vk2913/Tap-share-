package com.tapshare.app

import android.content.Context

object Store {
    const val DEFAULT_USER = "vshalkamat"
    private const val PREFS = "tapshare"

    fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cleanUser(raw: String): String {
        var u = raw.trim().removePrefix("@")
        u = u.substringBefore("?").substringBefore("#").trimEnd('/')
        if (u.contains("instagram.com/")) u = u.substringAfter("instagram.com/").substringBefore("/")
        return u.removePrefix("@").filter { it.isLetterOrDigit() || it == '.' || it == '_' }
    }

    fun url(c: Context): String {
        val p = prefs(c)
        if (p.getString("mode", "insta") == "custom") return (p.getString("custom", "") ?: "").trim()
        val u = cleanUser(p.getString("user", DEFAULT_USER) ?: DEFAULT_USER)
        return if (u.isEmpty()) "" else "https://www.instagram.com/$u"
    }

    fun fits(c: Context): Boolean = ndef(c).size <= 255

    private val PREFIXES = listOf("https://www." to 2, "http://www." to 1, "https://" to 4, "http://" to 3)

    private fun uriPayload(url: String): ByteArray {
        var code = 0
        var rest = url
        for ((prefix,id) in PREFIXES) if (url.startsWith(prefix)) { code=id; rest=url.removePrefix(prefix); break }
        return byteArrayOf(code.toByte()) + rest.toByteArray(Charsets.UTF_8)
    }

    fun ndef(c: Context): ByteArray {
        val url = url(c)
        if (url.isBlank()) return byteArrayOf(0,0)
        val payload = uriPayload(url)
        val rec = byteArrayOf(0xD1.toByte(),0x01,payload.size.toByte(),0x55) + payload
        if (rec.size > 255) return byteArrayOf(0,0)
        return byteArrayOf((rec.size shr 8).toByte(),rec.size.toByte()) + rec
    }
}
