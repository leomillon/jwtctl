package com.github.leomillon.jwt.generator

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
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
