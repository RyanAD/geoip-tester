package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.ip2location.IP2LocationWebService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class Ip2LocationProvider(
    @Value("\${ip2location.api.key}")
    private val apikey: String
) : GeoProvider {

    val iP2LocationWebService = IP2LocationWebService()

    init {
        iP2LocationWebService.Open(apikey, "WS24")
    }

    override fun name(): String {
        return "ip2location.com"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        iP2LocationWebService.IPQuery(ip).get("zip_code")?.asString
    }

}