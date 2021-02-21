package com.github.ryanad.config

import com.google.gson.Gson
import com.github.ryanad.repository.JsonFileLookupRepository
import com.github.ryanad.repository.LookupRepository
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class AppConfig {

    @Autowired
    private lateinit var gson: Gson

    @Bean
    fun httpClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Bean
    fun lookupRepository(): LookupRepository {
        return JsonFileLookupRepository(File("/tmp/lookups.json"), gson)
    }
}