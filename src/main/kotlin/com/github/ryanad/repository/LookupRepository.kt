package com.github.ryanad.repository

import com.github.ryanad.service.LookupResult

// super simple for now
interface LookupRepository {
    fun save(lookupResult: LookupResult)
    fun findAll(): List<LookupResult>
}