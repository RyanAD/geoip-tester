package com.github.ryanad.provider

import com.github.michaelbull.result.Result

interface GeoProvider {
    fun name(): String

    // TODO IP / Zip should be a type other than String
    fun lookupByIp(ip: String): Result<String?, Throwable>
}