package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader

// CURRENTLY UNUSED
class CachedGeoProviderDecorator(private val delegate: GeoProvider) : GeoProvider {

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .initialCapacity(10000)
        .build(object : CacheLoader<String, Result<String?, Throwable>>() {
            override fun load(ip: String): Result<String?, Throwable> {
                return delegate.lookupByIp(ip)
            }
        })

    override fun name(): String {
        return delegate.name()
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> {
        return cache.getUnchecked(ip)
    }
}

