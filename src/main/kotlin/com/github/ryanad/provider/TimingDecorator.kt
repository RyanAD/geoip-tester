package com.github.ryanad.provider

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.common.base.Stopwatch
import java.util.*
import java.util.concurrent.TimeUnit

// CURRENTLY UNUSED
class TimingDecorator(private val delegate: GeoProvider) : GeoProvider {
    // this puts a lock around list access which should generally be fine for a test
    // if we want better performance switch to CopyOnWriteArrayList
    val timings = Collections.synchronizedList(mutableListOf<Long>())
    override fun name(): String {
        return delegate.name()
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> {
        val sw = Stopwatch.createStarted()
        val result = delegate.lookupByIp(ip)
        // dont calculate timing if it failed
        if (result is Ok) {
            timings.add(sw.elapsed(TimeUnit.MILLISECONDS))
        }
        return result
    }
}