package com.github.leomillon.jwt.generator

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import mu.KotlinLogging
import org.slf4j.LoggerFactory

val log = KotlinLogging.logger {}

class Application {

    companion object {
        const val name = "jwtctl"
        val config by lazy {
            systemProperties() overriding
                    EnvironmentVariables()
            ConfigurationProperties.fromResource("config.properties")
        }
    }
}

fun changeLogLevel(level: Level?) {
    (LoggerFactory.getLogger(log.name) as Logger).level = level
}

object project : PropertyGroup() {
    val version by stringType
}
