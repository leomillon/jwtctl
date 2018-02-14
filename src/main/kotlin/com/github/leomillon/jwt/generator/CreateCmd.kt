package com.github.leomillon.jwt.generator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.common.orElse
import io.jsonwebtoken.CompressionCodec
import io.jsonwebtoken.CompressionCodecs
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Date

fun execGenerate(args: Array<String>) = wrapCommandBody(commandName = Command.CREATE.code) {

    val tokenParams = toTokenParams(args)

    val token = createToken(tokenParams)

    if (log.isInfoEnabled) {
        val parsedJwt = readToken(token, ignoreSignature = true, ignoreExpiration = true)
        log.info { "Header  : ${parsedJwt.header}" }
        log.info { "Body    : ${parsedJwt.body}" }

        log.info {
            "Generated token until : ${tokenParams.claims.expiration?.toInstant() ?: "no expiration date"}"
        }
    }

    displayAndExit(token, 0)
}

fun toTokenParams(args: Array<String>) = ArgParser(args).parseInto(::CreateCmdArgs).toTokenParams()

class CreateCmdArgs(parser: ArgParser) {

    val claimsFile by parser.storing("-f", "--claims-file",
            help = "JSON claims file path") {
        File(this)
    }
            .default<File?>(null)
            .addValidator {
                value?.let {
                    if (!it.exists()) throw InvalidArgumentException("Unable to find file at path ${it.path}")
                }
            }

    val claims by parser.option<MutableMap<String, String>>("-c", "--claim",
            argNames = listOf("NAME", "VALUE"),
            help = "claim to add to jwt body (override claims from file)") {
        value.orElse { mutableMapOf<String, String>() }.apply { put(arguments[0], arguments[1]) }
    }
            .default(mutableMapOf<String, String>())

    val headersFile by parser.storing("--headers-file",
            help = "JSON headers file path") {
        File(this)
    }
            .default<File?>(null)
            .addValidator {
                value?.let {
                    if (!it.exists()) throw InvalidArgumentException("Unable to find file at path ${it.path}")
                }
            }

    val headers by parser.option<MutableMap<String, String>>("--header",
            argNames = listOf("NAME", "VALUE"),
            help = "header to add to jwt header (override headers from file)") {
        value.orElse { mutableMapOf<String, String>() }.apply { put(arguments[0], arguments[1]) }
    }
            .default(mutableMapOf<String, String>())

    val duration by parser.storing("-d", "--duration",
            help = """
                set the duration of the token (expiration date = now + duration).
                Format : PTnHnMn.nS (ex: PT10H = 10 hours)
                """.trimIndent()) {
        Duration.parse(this)
    }
            .default<Duration?>(null)
            .addValidator {
                value?.let {
                    if (it.isNegative) throw IllegalArgumentException("Duration must be positive")
                }
            }

    val compressionCodec by parser.mapping(
            "--deflate" to CompressionCodecs.DEFLATE,
            "--gzip" to CompressionCodecs.GZIP,
            help = "the compression codec to use")
            .default<CompressionCodec?>(null)

    val signatureAlgorithm by parser.option<Pair<SignatureAlgorithm, String>?>("-s", "--signature",
            argNames = listOf("ALGORITHM", "SECRET"),
            help = "signature algorithm and base64 encoded secret key. Available algorithm : ${SignatureAlgorithm.values().filter { it.isHmac }.toList()}") {

        value?.let { throw IllegalArgumentException("Multiple signature algorithms in args") }
        value.orElse { Pair(SignatureAlgorithm.forName(arguments[0]), arguments[1]) }
    }
            .default<Pair<SignatureAlgorithm, String>?>(null)
            .addValidator {
                value?.let { if (!it.first.isHmac) throw InvalidArgumentException("Base64-encoded key bytes may only be specified for HMAC signatures") }
            }

    fun toTokenParams(): TokenParams {

        val now = Instant.now()

        val tokenClaims = Jwts.claims()
                .setIssuedAt(Date.from(now))

        duration?.let { tokenClaims.setExpiration(Date.from(now.plus(it))) }

        claimsFile?.let {
            try {
                tokenClaims.putAll(jacksonObjectMapper().readValue<Map<String, Any>>(it))
            } catch (e: Exception) {
                log.debug(e) { "Unable to parse claims file : ${e.message}" }
                throw InvalidArgumentException("Unable to parse claims file")
            }
        }

        tokenClaims.putAll(claims)

        val tokenHeaders = mutableMapOf<String, Any>()

        headersFile?.let {
            try {
                tokenHeaders.putAll(jacksonObjectMapper().readValue<Map<String, Any>>(it))
            } catch (e: Exception) {
                log.debug(e) { "Unable to parse headers file : ${e.message}" }
                throw InvalidArgumentException("Unable to parse headers file")
            }
        }

        tokenHeaders.putAll(headers)

        return TokenParams(
                claims = tokenClaims,
                headers = Jwts.header(tokenHeaders),
                signatureAlgAndSecret = signatureAlgorithm,
                compressionCodec = compressionCodec
        )
    }
}
