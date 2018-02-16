package com.github.leomillon.jwt.generator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import io.jsonwebtoken.JwtException
import java.io.File

class ReadArgs(parser: ArgParser) {

    val token by parser.positional("TOKEN",
            help = "the JWT token to read")

    val secret by parser.storing("-s", "--secret",
            help = "signature base64 encoded secret key")
            .default<String?>(null)

    val publicKeyFile by parser.storing("-f", "--public-key-file",
            help = "Public Key (PEM) file path") {
        File(this)
    }
            .default<File?>(null)
            .addValidator {
                value?.let {
                    if (!it.exists()) throw InvalidArgumentException("Unable to find file at path ${it.path}")
                }
            }

    val format by parser.mapping(
            "--standard" to OutputFormat.STANDARD,
            "--json" to OutputFormat.JSON,
            help = "output format (default: ${OutputFormat.STANDARD})"
    )
            .default(OutputFormat.STANDARD)

    val ignoreExpiration by parser.flagging("--ignore-expiration",
            help = "read the jwt claims/header even if the token is expired")

    val ignoreSignature by parser.flagging("--ignore-signature",
            help = "read the jwt claims/header even if the signature is invalid (displayed data cannot be trusted!!!)")
}

fun execRead(args: Array<String>) = wrapCommandBody(commandName = Command.READ.code) {
    val parsedArgs = ArgParser(args).parseInto(::ReadArgs)

    try {
        val parsedJwt = readToken(
                parsedArgs.token,
                parsedArgs.secret,
                parsedArgs.publicKeyFile,
                ignoreExpiration = parsedArgs.ignoreExpiration,
                ignoreSignature = parsedArgs.ignoreSignature)
        if (log.isInfoEnabled) {
            log.info { "Header  : ${parsedJwt.header}" }
            log.info { "Body    : ${parsedJwt.body}" }
            log.info { "Expired : ${parsedJwt.expired}" }
        }
        if (parsedJwt.ignoredSignature) {
            log.warn { "!!! Token signature has been ignored !!!" }
        }

        val result = when (parsedArgs.format) {
            OutputFormat.JSON -> jacksonObjectMapper().writeValueAsString(parsedJwt.body)
            else -> parsedJwt.body.toString()
        }

        displayAndExit(result, 0)
    } catch (e: JwtException) {
        log.debug("Error parsing JWT token", e)
        throw InvalidArgumentException("Unable to read JWT token")
    }
}

enum class OutputFormat {
    STANDARD,
    JSON
}
