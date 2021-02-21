package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class IpifyProvider(
    @Autowired private val httpClient: OkHttpClient,
    @Autowired private val gson: Gson,
    @Value("\${ipify.api.key}")
    private val apikey: String
) : GeoProvider {
    override fun name(): String {
        return "ipify.org"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        val url = "https://geo.ipify.org/api/v1?apiKey=$apikey&ipAddress=$ip"
        val request = Request.Builder()
            .url(url)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val content: String = response.body?.string().orEmpty()
            val locationMap = gson.fromJson(content, Map::class.java)?.get("location") as? Map<*, *>
            locationMap?.get("postalCode") as? String?
        }
    }
}