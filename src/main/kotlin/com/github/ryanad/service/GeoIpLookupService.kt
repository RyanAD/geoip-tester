package com.github.ryanad.service

import com.github.michaelbull.result.mapBoth
import com.google.common.base.Stopwatch
import com.google.common.cache.CacheBuilder
import com.google.common.math.Quantiles
import com.github.ryanad.provider.GeoProvider
import com.github.ryanad.repository.LookupRepository
import kotlinx.coroutines.*
import mu.KotlinLogging
import okhttp3.internal.toImmutableList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.math.max


@Service
class GeoIpLookupService(
    @Autowired private val lookupProviders: List<GeoProvider>,
    @Autowired private val lookupRepository: LookupRepository,
    @Autowired private val distanceService: DistanceService
) {
    private val logger = KotlinLogging.logger {}
    private val coroutineDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private val cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .initialCapacity(10000)
        .build<String, LookupResult>()

    // this puts a lock around list access which should generally be fine for a test
    // if we want better performance switch to CopyOnWriteArrayList
    //private val executionTimings =
    //    lookupProviders.associate { it.name() to Collections.synchronizedList(mutableListOf<Long>()) }

    @PreDestroy
    fun shutdown() {
        coroutineScope.cancel()
        coroutineDispatcher.close()
    }

    @PostConstruct
    fun loadSavedLookups() {
        val pastLookups = lookupRepository.findAll()
        pastLookups.forEach { lookup ->
            val key = cacheKey(lookup.provider, lookup.ipAddress, lookup.expectedAddress)
            cache.put(key, lookup)
        }

        // now that we have the cache loaded, back-testing any new providers is as simple as looping through
        // old lookups and rerunning them.
        coroutineScope.launch {
            logger.info { "Backtesting any new providers" }
            pastLookups.forEach { lookupByIp(it.ipAddress, it.expectedAddress) }
            logger.info { "Done backtesting, back filling distance measurement" }
            pastLookups
                .filterNot { it.hadError }
                .forEach { lookup ->
                    if (lookup.distanceInMeters == -1L) {
                        logger.info { "Back filling distance for $lookup" }
                        val key = cacheKey(lookup.provider, lookup.ipAddress, lookup.expectedAddress)
                        val distance = distanceService.calculateDistance(lookup.postalCode.orEmpty(), lookup.expectedAddress)
                        val updatedResult = lookup.copy(distanceInMeters = distance ?: -2L)
                        cache.put(key, updatedResult)
                        lookupRepository.save(updatedResult)
                    }
                }
            logger.info { "Done back filling distance measurement" }
        }
    }

    suspend fun dumpAll(): List<LookupResult> {
        return cache.asMap().values.toList()
    }

    suspend fun lookupByIp(ipAddress: String, expectedAddress: String): List<LookupResult> {
        val deferredResults = lookupProviders.map { provider ->
            coroutineScope.async {
                internalLookupByIp(ipAddress, expectedAddress, provider)
            }
        }

        val results = deferredResults.map { it.await() }.toList()
        /*for (result in results) {
            // don't track execution time on errors
            if (!result.hadError) {
                executionTimings[result.provider]!!.add(result.durationMs)
            }
        }*/
        return results
    }

    suspend fun latencyStats(): Map<String, ProviderLatency> {
        val executionTimings = dumpAll()
            .filterNot { it.hadError }
            .groupBy { it.provider }
            .mapValues { it.value.map { it.durationMs } }


        return executionTimings
            .map { entry ->
                val percentiles = Quantiles.scale(1000).indexes(500, 900, 950, 990, 999).compute(entry.value)
                ProviderLatency(
                    provider = entry.key,
                    median = percentiles[500]!!,
                    ninetieth = percentiles[900]!!,
                    ninetyFifth = percentiles[950]!!,
                    threeNines = percentiles[999]!!
                )
            }
            .associateBy { it.provider }
    }

    suspend fun statsByMatchingPostalCode(): List<ProviderStats> {

        val latencies = latencyStats()

        // remove errors for now
        val pastLookups = dumpAll().filterNot { it.hadError }
        val stats = pastLookups
            .groupBy { it.provider }
            .map { entries ->
                val total = entries.value.size
                val matched = entries.value.count { result ->
                    val detectedPc = result.postalCode.orEmpty()
                    if (detectedPc.length < 5) {
                        false
                    } else {
                        detectedPc.substring(0..4).equals(result.expectedAddress)
                    }
                }
                ProviderStats(entries.key, total, matched.toDouble() / max(total, 1), latencies[entries.key]!!)
            }

        return stats.sortedByDescending { it.percentAccurate }
    }

    suspend fun statsByNearestDistance(): List<ProviderStats> {

        val latencies = latencyStats()

        // remove errors for now
        val pastLookups = dumpAll().filterNot { it.hadError || it.distanceInMeters < 0 }
        val pastLookupsByProvider = pastLookups.groupBy { it.provider }
        val lookupsWon = mutableMapOf<String, Int>()
        pastLookups
            .groupBy { it.ipAddress to it.expectedAddress }
            .forEach { entries ->
                //only for debug
                val winner = entries.value.minByOrNull { it.distanceInMeters }!!
                lookupsWon[winner.provider] = lookupsWon.getOrPut(winner.provider, { 0 }).inc()
            }

        return lookupsWon
            .map { entry ->
                val totalLookups = pastLookupsByProvider[entry.key]!!.size
                ProviderStats(
                    entry.key,
                    totalLookups,
                    entry.value.toDouble() / totalLookups,
                    latencies[entry.key]!!
                )
            }
            .sortedByDescending { it.percentAccurate }
    }


    private suspend fun internalLookupByIp(
        ipAddress: String,
        expectedAddress: String,
        geoProvider: GeoProvider
    ): LookupResult {
        logger.info { "Looking up in thread ${Thread.currentThread()}" }

        // check cache, technically two threads could get past this check and we'd make the call N times
        // rare and worth it for the simplicity of this cache impl
        val cacheKey = cacheKey(geoProvider.name(), ipAddress, expectedAddress)
        val cachedRes = cache.getIfPresent(cacheKey)
        if (cachedRes != null) return cachedRes.copy(cachedResponse = true)


        val sw = Stopwatch.createStarted()
        val result = geoProvider.lookupByIp(ipAddress)
        val elapsedMs = sw.elapsed(TimeUnit.MILLISECONDS)

        val lookupResult = result.mapBoth(
            { postalCode ->
                LookupResult(
                    false,
                    geoProvider.name(),
                    ipAddress,
                    postalCode,
                    expectedAddress,
                    distanceService.calculateDistance(postalCode.orEmpty(), expectedAddress) ?: -2L,
                    elapsedMs
                )
            },
            { LookupResult(true, geoProvider.name(), ipAddress, null, expectedAddress, -2L, elapsedMs) }
        )
        cache.put(cacheKey, lookupResult)
        // no need to wait for the save to finish
        coroutineScope.launch { lookupRepository.save(lookupResult) }
        return lookupResult
    }

    private fun cacheKey(provider: String, ipAddress: String, expectedAddress: String): String {
        return "$provider|$ipAddress|||$expectedAddress".toLowerCase()
    }
}

data class LookupResult(
    val hadError: Boolean,
    val provider: String,
    val ipAddress: String,
    val postalCode: String?,
    val expectedAddress: String,
    val distanceInMeters: Long,
    val durationMs: Long,
    val timestamp: Long = Instant.now().epochSecond,
    val cachedResponse: Boolean = false
)

data class ProviderLatency(
    val provider: String,
    val median: Double,
    val ninetieth: Double,
    val ninetyFifth: Double,
    val threeNines: Double
)

data class ProviderStats(
    val provider: String,
    val totalLookups: Int,
    val percentAccurate: Double,
    val providerLatency: ProviderLatency
)