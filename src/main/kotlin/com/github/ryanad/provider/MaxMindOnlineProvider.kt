package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.maxmind.geoip2.WebServiceClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.InetAddress

@Service
class MaxMindOnlineProvider(
    @Value("\${maxmind.api.key}")
    private val apikey: String
) : GeoProvider {

    private val maxMindClient = WebServiceClient.Builder(131722, apikey)
        .build()

    override fun name(): String {
        return "maxmind.com"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        maxMindClient.city(InetAddress.getByName(ip)).postal.code
    }
}