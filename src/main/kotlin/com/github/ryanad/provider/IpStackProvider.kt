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
class IpStackProvider(
    @Autowired private val httpClient: OkHttpClient,
    @Autowired private val gson: Gson,
    @Value("\${ipstack.api.key}")
    private val apikey: String
) : GeoProvider {



    // NOTE this looks the same as IpAPI.com as well...
    override fun name(): String {
        return "ipstack.com"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        val url = "http://api.ipstack.com/$ip?access_key=$apikey"
        val request = Request.Builder()
            .url(url)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val content: String = response.body?.string().orEmpty()
            gson.fromJson(content, Map::class.java)?.get("zip") as? String?
        }
    }
}