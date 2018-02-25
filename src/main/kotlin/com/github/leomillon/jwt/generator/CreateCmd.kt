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

    private val claimsFile by parser.storing("-f", "--claims-file",
            help = "JSON claims file path") {
        File(this)
    }
            .default<File?>(null)
            .addValidator {
                value?.let {
                    if (!it.exists()) throw InvalidArgumentException("Unable to find file at path ${it.path}")
                }
            }

    private val claims by parser.option<MutableMap<String, String>>("-c", "--claim",
            argNames = listOf("NAME", "VALUE"),
            isRepeating = true,
            help = "claim to add to jwt body (override claims from file)") {
        value.orElse { mutableMapOf<String, String>() }.apply { put(arguments[0], arguments[1]) }
    }
            .default(mutableMapOf<String, String>())

    private val headersFile by parser.storing("--headers-file",
            help = "JSON headers file path") {
        File(this)
    }
            .default<File?>(null)
            .addValidator {
                value?.let {
                    if (!it.exists()) throw InvalidArgumentException("Unable to find file at path ${it.path}")
                }
            }

    private val headers by parser.option<MutableMap<String, String>>("--header",
            argNames = listOf("NAME", "VALUE"),
            isRepeating = true,
            help = "header to add to jwt header (override headers from file)") {
        value.orElse { mutableMapOf<String, String>() }.apply { put(arguments[0], arguments[1]) }
    }
            .default(mutableMapOf<String, String>())

    private val duration by parser.storing("-d", "--duration",
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

    private val compressionCodec by parser.mapping(
            "--deflate" to CompressionCodecs.DEFLATE,
            "--gzip" to CompressionCodecs.GZIP,
            help = "the compression codec to use")
            .default<CompressionCodec?>(null)

    private val hmacSignature by parser.option<Pair<SignatureAlgorithm, String>?>("--hmac-sign",
            argNames = listOf("HMAC_ALG", "SECRET"),
            help = """
                HMAC signature algorithm and base64 encoded secret key.
                Available algorithms : ${SignatureAlgorithm.values().filter { it.isHmac }.toList()}
                """.trimIndent()) {

        value.orElse { Pair(SignatureAlgorithm.forName(arguments[0]), arguments[1]) }
    }
            .default<Pair<SignatureAlgorithm, String>?>(null)
            .addValidator {
                value?.let { if (!it.first.isHmac) throw InvalidArgumentException("Invalid HMAC algorithm") }
            }

    private val rsaSignature by parser.option<Pair<SignatureAlgorithm, File>?>("--rsa-sign",
            argNames = listOf("RSA_ALG", "FILE_PATH"),
            help = """
                RSA signature algorithm and Private Key (PEM) file path.
                Available algorithms : ${SignatureAlgorithm.values().filter { it.isRsa }.toList()}
                """.trimIndent()) {

        value.orElse { Pair(SignatureAlgorithm.forName(arguments[0]), File(arguments[1])) }
    }
            .default<Pair<SignatureAlgorithm, File>?>(null)
            .addValidator {
                value?.let {
                    if (!it.first.isRsa) throw InvalidArgumentException("Invalid RSA algorithm")
                    if (!it.second.exists()) throw InvalidArgumentException("Unable to find file at path ${it.second}")
                }
            }

    private val pemFilePassword by parser.storing("-p", "--password",
            help = """
                the password of the encrypted PEM file.
                Will be asked interactively if not provided via this parameter.
                """.trimIndent())
            .default<String?>(null)

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

        val passwordProvider = pemFilePassword
                ?.let { { it.toCharArray() } }
                ?: {
                    print("Enter password ${rsaSignature?.second?.path?.let { "($it) " }}: ")
                    Application.askPassword()
                }

        return TokenParams(
                claims = tokenClaims,
                headers = Jwts.header(tokenHeaders),
                hmacAlgAndSecret = hmacSignature,
                rsaAlgAndFile = rsaSignature,
                compressionCodec = compressionCodec,
                passwordProvider = passwordProvider
        )
    }
}
