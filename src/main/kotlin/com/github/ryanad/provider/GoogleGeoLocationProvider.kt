package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.GeolocationApi
import com.google.maps.model.AddressComponentType
import com.google.maps.model.GeolocationPayload

// DISABLED, Cannot pass in an IP Address
class GoogleGeoLocationProvider : GeoProvider {
    private val context = GeoApiContext.Builder()
        .apiKey("")
        .build()

    override fun name(): String {
        return "googlemaps.com"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        lookupInternal(ip)
    }

    private fun lookupInternal(ip: String): String? {
        val payload = GeolocationPayload.GeolocationPayloadBuilder().ConsiderIp(true).createGeolocationPayload()
        val geolocation = GeolocationApi.geolocate(context, payload).await() ?: return null
        val location = GeocodingApi.reverseGeocode(context, geolocation.location).await() ?: return null
        val interestedTypes = setOf(AddressComponentType.POSTAL_CODE, AddressComponentType.POSTAL_CODE_PREFIX)
        for (result in location) {
            val postalCodeComponent =
                result.addressComponents.find { it.types.toSet().intersect(interestedTypes).isNotEmpty() }
            return postalCodeComponent?.shortName
        }
        return null
    }
}