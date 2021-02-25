package com.github.ryanad.service

import com.google.maps.DistanceMatrixApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TrafficModel
import com.google.maps.model.TravelMode
import com.google.maps.model.Unit
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DistanceService(
    @Value("\${google.maps.api.key}")
    private val apikey: String
) {
    private val context = GeoApiContext.Builder()
        .apiKey(apikey)
        .build()

    fun calculateDistance(originAddr: String, destinationAddr: String): Long? {
        // DISABLED UNTIL I CAN ENABLE BILLING AT GOOGLE
        //if (true) return -1L

        if (originAddr.isBlank() || destinationAddr.isBlank()) return null

        val request = DistanceMatrixApi.getDistanceMatrix(context, arrayOf(originAddr), arrayOf(destinationAddr))
        request.trafficModel(TrafficModel.OPTIMISTIC)
        request.units(Unit.IMPERIAL)
        request.mode(TravelMode.DRIVING)
        request.departureTime(Instant.now())

        val result = request.await()
        return result.rows.first().elements.first().distance.inMeters
    }
}