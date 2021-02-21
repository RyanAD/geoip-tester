package com.github.ryanad.provider

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress

@Service
class MaxMindDbProvider : GeoProvider {
    private val maxmindDb = DatabaseReader.Builder(File("GeoIP2-City.mmdb")).withCache(CHMCache()).build()
    override fun name(): String {
        return "maxmind.file"
    }

    override fun lookupByIp(ip: String): Result<String?, Throwable> = runCatching {
        val city = maxmindDb.city(InetAddress.getByName(ip))
        city.postal.code
    }
}