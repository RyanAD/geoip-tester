package com.github.ryanad.web

import com.github.ryanad.service.GeoIpLookupService
import com.github.ryanad.service.LookupResult
import com.github.ryanad.service.ProviderStats
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
class LookupController(@Autowired private val lookupService: GeoIpLookupService) {
    @GetMapping("/lookup")
    suspend fun lookup(
        @RequestParam("ipAddress") ipAddress: String,
        @RequestParam("expectedAddress") expectedAddress: String
    ): List<LookupResult> {
        return lookupService.lookupByIp(ipAddress, expectedAddress)
    }

    @GetMapping("/stats")
    suspend fun stats(): List<ProviderStats> {
        return lookupService.statsByMatchingPostalCode()
    }

    @GetMapping("/statsByDistance")
    suspend fun statsByDistance(): List<ProviderStats> {
        return lookupService.statsByNearestDistance()
    }

    @GetMapping("/dump")
    suspend fun dump(): List<LookupResult> {
        return lookupService.dumpAll()
    }
}