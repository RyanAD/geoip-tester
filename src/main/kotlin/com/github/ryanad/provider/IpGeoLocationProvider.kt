package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import io.ipgeolocation.api.GeolocationParams
import io.ipgeolocation.api.IPGeolocationAPI
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class IpGeoLocationProvider(
    @Value("\${ipgeolocation.api.key}")
    private val apikey: String
) : GeoProvider {
    private val client = IPGeolocationAPI(apikey)
    override fun name(): String {
        return "ipgeolocation.io"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        val geoParams = GeolocationParams()
        geoParams.ipAddress = ip
        geoParams.fields = "geo"

        client.getGeolocation(geoParams).zipCode
    }
}