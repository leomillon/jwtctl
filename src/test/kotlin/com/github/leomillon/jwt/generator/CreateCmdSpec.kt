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

object CreateCmdSpec : Spek({

    describe("Help rendering") {
        it("should throw exception to display help") {
            shouldThrow<ShowHelpException> {
                toTokenParams(arrayOf("--help"))
            }.run {
                message shouldBe "Help was requested"
                val help = StringWriter().apply { printUserMessage(this, "some_name", 0) }.toString()
                help shouldBe """
usage: some_name [-h] [-f CLAIMS_FILE] [-c NAME VALUE] [--headers-file HEADERS_FILE] [--header NAME VALUE] [-d DURATION] [--deflate] [-s ALGORITHM SECRET]

optional arguments:
  -h, --help                                          show this help message and exit

  -f CLAIMS_FILE, --claims-file CLAIMS_FILE           JSON claims file path

  -c NAME VALUE, --claim NAME VALUE                   claim to add to jwt body (override claims from file)

  --headers-file HEADERS_FILE                         JSON headers file path

  --header NAME VALUE                                 header to add to jwt header (override headers from file)

  -d DURATION, --duration DURATION                    set the duration of the token (expiration date = now + duration).
                                                      Format : PTnHnMn.nS (ex: PT10H = 10 hours)

  --deflate, --gzip                                   the compression codec to use

  -s ALGORITHM SECRET, --signature ALGORITHM SECRET   signature algorithm and base64 encoded secret key. Available algorithm : [HS256, HS384, HS512]

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
            result.signatureAlgAndSecret shouldBe null
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

        it("should set HMAC signature algorithm and secret") {
            val secret = "some_secret"
            val result = toTokenParams(arrayOf("--signature", "HS512", secret))

            result.signatureAlgAndSecret shouldNotBe null
            result.signatureAlgAndSecret!!.first shouldBe SignatureAlgorithm.HS512
            result.signatureAlgAndSecret!!.second shouldBe secret
        }

        it("should set HMAC signature algorithm and secret") {
            forAll(listOf(
                    SignatureAlgorithm.HS256,
                    SignatureAlgorithm.HS384,
                    SignatureAlgorithm.HS512
            )) {
                val secret = "some_secret"
                val result = toTokenParams(arrayOf("--signature", it.name, secret))

                result.signatureAlgAndSecret shouldNotBe null
                result.signatureAlgAndSecret!!.first shouldBe it
                result.signatureAlgAndSecret!!.second shouldBe secret
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

    describe("Unsupported describes...") {
        it("should get error for non-HMAC signature algorithms") {
            forAll(listOf(
                    SignatureAlgorithm.ES256,
                    SignatureAlgorithm.RS256,
                    SignatureAlgorithm.PS256,
                    SignatureAlgorithm.NONE
            )) {
                val exception = shouldThrow<InvalidArgumentException> {
                    toTokenParams(arrayOf("--signature", it.name, "some_secret"))
                }
                exception.message shouldBe "Base64-encoded key bytes may only be specified for HMAC signatures"
            }
        }
    }
})
