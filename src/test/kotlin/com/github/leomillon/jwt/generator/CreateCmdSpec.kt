package com.github.leomillon.jwt.generator

import com.github.leomillon.jwt.generator.Utils.closeTo
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.ShowHelpException
import io.jsonwebtoken.CompressionCodecs
import io.jsonwebtoken.SignatureAlgorithm
import io.kotlintest.forAll
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.matchers.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.StringWriter
import java.time.Instant
import java.util.Arrays

object CreateCmdSpec : Spek({

    describe("Help rendering") {
        it("should throw exception to display help") {
            shouldThrow<ShowHelpException> {
                toTokenParams(arrayOf("--help"))
            }.run {
                message shouldBe "Help was requested"
                val help = StringWriter().apply { printUserMessage(this, "some_name", 0) }.toString()
                help shouldBe """
usage: some_name [-h] [-f CLAIMS_FILE] [-c NAME VALUE]... [--headers-file HEADERS_FILE] [--header NAME VALUE]... [-d DURATION] [--deflate] [--hmac-sign HMAC_ALG SECRET] [--rsa-sign RSA_ALG FILE_PATH] [-p PASSWORD]

optional arguments:
  -h, --help                                  show this help message and exit

  -f CLAIMS_FILE, --claims-file CLAIMS_FILE   JSON claims file path

  -c NAME VALUE, --claim NAME VALUE           claim to add to jwt body (override claims from file)

  --headers-file HEADERS_FILE                 JSON headers file path

  --header NAME VALUE                         header to add to jwt header (override headers from file)

  -d DURATION, --duration DURATION            set the duration of the token (expiration date = now + duration).
                                              Format : PTnHnMn.nS (ex: PT10H = 10 hours)

  --deflate, --gzip                           the compression codec to use

  --hmac-sign HMAC_ALG SECRET                 HMAC signature algorithm and base64 encoded secret key.
                                              Available algorithms : [HS256, HS384, HS512]

  --rsa-sign RSA_ALG FILE_PATH                RSA signature algorithm and Private Key (PEM) file path.
                                              Available algorithms : [RS256, RS384, RS512, PS256, PS384, PS512]

  -p PASSWORD, --password PASSWORD            the password of the encrypted PEM file.
                                              Will be asked interactively if not provided via this parameter.

"""
                        .trimStart()
            }
        }
    }

    describe("Parse string args to token params") {
        it("No args should set issuedAt claim to now") {
            val now = Instant.now()
            val result = toTokenParams(emptyArray())

            result.claims.issuedAt shouldBe closeTo(now)
            result.claims.size shouldBe 1

            result.headers.size shouldBe 0
            result.hmacAlgAndSecret shouldBe null
            result.compressionCodec shouldBe null
        }

        it("should set claims/headers from files/direct args") {
            val now = Instant.now()

            val result = toTokenParams(arrayOf(
                    "--claims-file", "/claims.json".resourcePath(),
                    "--claim", "myCustomClaimName", "myCustomClaimValue",
                    "--claim", "claimToOverrideName", "claimToOverrideValue",
                    "--headers-file", "/headers.json".resourcePath(),
                    "--header", "myCustomHeaderName", "myCustomHeaderValue",
                    "--header", "headerToOverrideName", "headerToOverrideValue"
            ))

            result.claims.issuedAt shouldBe closeTo(now)
            result.claims["myCustomClaimName"] shouldBe "myCustomClaimValue"
            result.claims["fileClaimsName"] shouldBe "fileClaimsValue"
            result.claims["claimToOverrideName"] shouldBe "claimToOverrideValue"
            result.claims.size shouldBe 4

            result.headers["myCustomHeaderName"] shouldBe "myCustomHeaderValue"
            result.headers["fileHeadersName"] shouldBe "fileHeadersValue"
            result.headers["headerToOverrideName"] shouldBe "headerToOverrideValue"
            result.headers.size shouldBe 3
        }

        describe("Signature features") {

            it("should set HMAC signature algorithm and secret") {
                forAll(
                        SignatureAlgorithm
                                .values()
                                .filter { it.isHmac }
                ) {
                    val secret = "some_secret"
                    val result = toTokenParams(arrayOf("--hmac-sign", it.name, secret))

                    result.hmacAlgAndSecret shouldNotBe null
                    result.hmacAlgAndSecret!!.first shouldBe it
                    result.hmacAlgAndSecret!!.second shouldBe secret
                }
            }

            it("should get error for non-HMAC signature algorithms") {
                forAll(
                        SignatureAlgorithm
                                .values()
                                .filterNot { it.isHmac }
                ) {
                    val exception = shouldThrow<InvalidArgumentException> {
                        toTokenParams(arrayOf("--hmac-sign", it.name, "some_secret"))
                    }
                    exception.message shouldBe "Invalid HMAC algorithm"
                }
            }

            it("should set RSA signature algorithm and private") {
                forAll(
                        SignatureAlgorithm
                                .values()
                                .filter { it.isRsa }
                ) {
                    val pemFilePath = "/private_key_1.pem".resourcePath()
                    val result = toTokenParams(arrayOf("--rsa-sign", it.name, pemFilePath))

                    result.rsaAlgAndFile shouldNotBe null
                    result.rsaAlgAndFile!!.first shouldBe it
                    result.rsaAlgAndFile!!.second.path shouldBe pemFilePath
                }
            }

            it("should get error for non-RSA signature algorithms") {
                forAll(
                        SignatureAlgorithm
                                .values()
                                .filterNot { it.isRsa }
                ) {
                    val exception = shouldThrow<InvalidArgumentException> {
                        toTokenParams(arrayOf("--rsa-sign", it.name, "/private_key_1.pem".resourcePath()))
                    }
                    exception.message shouldBe "Invalid RSA algorithm"
                }
            }

            it("should set password") {
                val password = "changeit"
                val result = toTokenParams(arrayOf("--password", password))

                Arrays.equals(result.passwordProvider?.invoke()!!, password.toCharArray()) shouldBe true
            }

            describe("Interactive input") {

                val password = "changeit"
                beforeGroup {
                    System.setIn(password.byteInputStream())
                }

                it("should have default password provider if not set") {
                    val result = toTokenParams(emptyArray())

                    Arrays.equals(result.passwordProvider?.invoke()!!, password.toCharArray()) shouldBe true
                }

                afterGroup {
                    System.setIn(System.`in`)
                }
            }
        }

        it("should set compression codec") {
            forAll(listOf(
                    Pair("--deflate", CompressionCodecs.DEFLATE),
                    Pair("--gzip", CompressionCodecs.GZIP)
            )) {
                val result = toTokenParams(arrayOf(it.first))

                result.compressionCodec shouldBe it.second
            }
        }
    }
})
