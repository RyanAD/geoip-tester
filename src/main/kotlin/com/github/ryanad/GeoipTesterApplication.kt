package com.github.ryanad

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GeoipTesterApplication

fun main(args: Array<String>) {
    runApplication<GeoipTesterApplication>(*args)
}
