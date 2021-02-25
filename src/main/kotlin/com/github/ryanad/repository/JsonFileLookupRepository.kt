package com.github.ryanad.repository

import com.google.gson.Gson
import com.github.ryanad.service.LookupResult
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.annotation.PreDestroy

// super hacky, but don't feel like adding sqlite or something else for now
class JsonFileLookupRepository(private val file: File, private val gson: Gson) : LookupRepository {
    private val writer = FileOutputStream(file, true).bufferedWriter()

    override fun save(lookupResult: LookupResult) {
        synchronized(this) {
            writer.appendLine(gson.toJson(lookupResult))
        }
    }

    override fun findAll(): List<LookupResult> {
        synchronized(this) {
            writer.flush()
            return file.bufferedReader().use { reader ->
                reader.lineSequence()
                    .filterNot { it.isBlank() }
                    .map { gson.fromJson(it, LookupResult::class.java) }
                        // timestamp could be null from old Json representations
                    .map { if(it.timestamp == null) it.copy(timestamp = Instant.now().epochSecond) else it }
                    .toList()
            }
        }
    }

    @PreDestroy
    fun close() {
        writer.close()
    }
}