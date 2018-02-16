package com.github.leomillon.jwt.generator

import com.xenomachina.argparser.InvalidArgumentException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SignatureException
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.matchers.startWith
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.time.Instant
import java.util.Date

object JwtServiceSpec : Spek({

    describe("Token reading") {

        val jwt = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NjMyfQ."
        val expiredJwt = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5LCJleHAiOjE1MTc0Mzk1ODl9."

        val secret = "some_secret"
        val hmacJws = "eyJhbGciOiJIUzUxMiJ9" +
                ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NzY0fQ" +
                ".3SlJH4_hojObopvtrENgJ-TgqbuXvBH4tqXwiFbH7TVhg0ujx50UvvFThU5jQV_S4jyt9TXVYB-55xc9_Quhbw"
        val rsaJws = "eyJhbGciOiJSUzUxMiJ9" +
                ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ" +
                ".kPaD3_duc22VKAVi_fsmH4KCXhLnh6cf8OC_aTKaeSDIW2M3JjPHVaMf2kFbtS57Sa4SO0ujvvw5sbIUtmlaFv4s" +
                "dNAnT1IFTsKjUCeSvkuCHoFxBjRZDhOohiHiND_tsT1n7zLD3CpX0UVn4PGP-YTeidr3c07Fw7WfYXn-aPyySgEgQ" +
                "PnklQMxyGih8dmGHSuRQ-7U_sKJnZ_zftv7M-YxdtjwJicBXLzqGVPwz4E4YMdRU921X8xQyu_faqIhaUHpkHtdhf" +
                "N3EElfq4R-ReneHpFePCOIklPRByZNkvwRxK_oxJukmqQDMTS_hyYqO-NJued4NxH0dOmMb_ENxw"
        val expiredHmacJws = "eyJhbGciOiJIUzUxMiJ9" +
                ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5MTAyLCJleHAiOjE1MTc0MzkxMDJ9" +
                ".PNoZ4DS-OipT3UO6LJmr9TU4MBH8xbUEMR99Fxhwu616otq9y-07KdpeiTijL1hfJCtS2CfNPkWhkGzYu-ESiQ"
        val compressedHmacJws = "eyJhbGciOiJIUzUxMiIsInppcCI6IkRFRiJ9" +
                ".eNqqViouTVKyUipJLS7RLcnPTs1T0lHKTCxRsjI0NTQ3MbawMDepBQAAAP__" +
                ".IRxcAWxKFawsg56PuQad6eZ4TCqLU0CnJP03AH4PVExU74oA9G9Le2AwbQoUxovlT5uptmhL8vqNb9LCdubVUg"

        it("should read JWT token") {
            val result = readToken(jwt)

            result.header.toString() shouldBe "{alg=none}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438632}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should not read expired JWT token") {
            val exception = shouldThrow<InvalidArgumentException> {
                readToken(expiredJwt)
            }
            exception.message shouldBe startWith("JWT expired at")
        }

        it("should not read expired JWS token") {
            val exception = shouldThrow<InvalidArgumentException> {
                readToken(expiredHmacJws, base64EncodedSecret = secret)
            }
            exception.message shouldBe startWith("JWT expired at")
        }

        it("should read HMAC JWS token") {
            val result = readToken(hmacJws, base64EncodedSecret = secret)

            result.header.toString() shouldBe "{alg=HS512}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438764}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should not read HMAC JWS token without secret") {
            val exception = shouldThrow<IllegalArgumentException> {
                readToken(hmacJws)
            }
            exception.message shouldBe startWith("A signing key must be specified if the specified JWT is digitally signed.")
        }

        it("should not read HMAC JWS token with wrong secret") {
            val exception = shouldThrow<SignatureException> {
                readToken(hmacJws, "WRONG!!!")
            }
            exception.message shouldBe startWith("JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.")
        }

        it("should read RSA JWS token") {
            val result = readToken(rsaJws, pemFile = "/public_key_1.pem".resourceFile())

            result.header.toString() shouldBe "{alg=RS512}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517439589}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should not read RSA JWS token without public file") {
            val exception = shouldThrow<IllegalArgumentException> {
                readToken(rsaJws)
            }
            exception.message shouldBe startWith("A signing key must be specified if the specified JWT is digitally signed.")
        }

        it("should not read RSA JWS token with wrong public key") {
            val exception = shouldThrow<SignatureException> {
                readToken(rsaJws, pemFile = "/public_key_2.pem".resourceFile())
            }
            exception.message shouldBe startWith("JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.")
        }

        it("should read compressed JWS token") {
            val result = readToken(compressedHmacJws, secret)

            result.header.toString() shouldBe "{alg=HS512, zip=DEF}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438874}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should read JWS ignoring signature") {
            val result = readToken(compressedHmacJws, ignoreSignature = true)

            result.header.toString() shouldBe "{alg=HS512, zip=DEF}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438874}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe true
        }

        it("should read expired JWT ignoring expiration") {
            val result = readToken(expiredJwt, ignoreExpiration = true)

            result.header.toString() shouldBe "{alg=none}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517439589, exp=1517439589}"
            result.ignoredSignature shouldBe false
            result.expired shouldBe true
        }

        it("should read expired JWS ignoring signature and expiration") {
            val result = readToken(expiredHmacJws, ignoreSignature = true, ignoreExpiration = true)

            result.header.toString() shouldBe "{alg=HS512}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517439102, exp=1517439102}"
            result.expired shouldBe true
            result.ignoredSignature shouldBe true
        }
    }

    describe("Token creation") {

        val iatField: Long = 1517439589
        val claims = Jwts.claims()
                .setSubject("test-token")
                .setIssuedAt(Date.from(Instant.ofEpochSecond(iatField)))

        it("shoud not create empty JWT") {
            val exception = shouldThrow<IllegalStateException> {
                createToken(TokenParams())
            }
            exception.message shouldBe startWith("Either 'payload' or 'claims' must be specified")
        }

        it("should create JWT") {
            val result = createToken(TokenParams(claims))

            result shouldBe "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ."
        }

        it("should create HMAC JWS") {
            val result = createToken(TokenParams(
                    claims,
                    hmacAlgAndSecret = Pair(SignatureAlgorithm.HS512, "some_secret")
            ))

            result shouldBe "eyJhbGciOiJIUzUxMiJ9" +
                    ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ" +
                    ".uPsQwXp_s4iVZOE56frIZE8lx-zNGT8peZ4kT6yO1yeUnzJFZ7d47VSkSGjZvohha2v_UZ-bbicQw0J2ir6jNg"
        }

        it("should create RSA JWS with PEM private key without password") {
            val result = createToken(TokenParams(
                    claims,
                    rsaAlgAndFile = Pair(SignatureAlgorithm.RS512, "/private_key_2_no_pass.pem".resourceFile())
            ))

            result shouldBe "eyJhbGciOiJSUzUxMiJ9" +
                    ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ" +
                    ".VPIwV31_A1FB-Ejwypsajo2bWxf_DRsX6fWeHaoPGdSQTcvbiusUS_nc5siNbvB6rb_XXCDGmYiPZ0pSU8Reota" +
                    "HOKrNwFC6xfnvBGhneWKZ6g2HhiIboPSzNDwId3z9IsCMlhPPpVMM2NBTaRFfsyOrwGL9YK3j7U5qyzJ8g7jWacS" +
                    "loIJ18fMncbY6bZvIfGVP_CQ8m-uUZre1ik_uHctdlVZcarWp6AZNfohqx0oBAg31vBXpKTvhUXIc-7zwjnPILf1" +
                    "zAWcJmVy_D77nwK6f7kuTp68jtypkHnNi5-EeLjypXEbqDPQF6Zvt_GrYBYogV98CrgQHKR76J0zkOw"
        }

        it("should create RSA JWS with PEM private key with password") {
            val result = createToken(TokenParams(
                    claims,
                    rsaAlgAndFile = Pair(SignatureAlgorithm.RS512, "/private_key_1.pem".resourceFile()),
                    passwordProvider = { "changeit".toCharArray() }
            ))

            result shouldBe "eyJhbGciOiJSUzUxMiJ9" +
                    ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ" +
                    ".kPaD3_duc22VKAVi_fsmH4KCXhLnh6cf8OC_aTKaeSDIW2M3JjPHVaMf2kFbtS57Sa4SO0ujvvw5sbIUtmlaFv4s" +
                    "dNAnT1IFTsKjUCeSvkuCHoFxBjRZDhOohiHiND_tsT1n7zLD3CpX0UVn4PGP-YTeidr3c07Fw7WfYXn-aPyySgEgQ" +
                    "PnklQMxyGih8dmGHSuRQ-7U_sKJnZ_zftv7M-YxdtjwJicBXLzqGVPwz4E4YMdRU921X8xQyu_faqIhaUHpkHtdhf" +
                    "N3EElfq4R-ReneHpFePCOIklPRByZNkvwRxK_oxJukmqQDMTS_hyYqO-NJued4NxH0dOmMb_ENxw"
        }

        it("shoud not create RSA JWS with PEM private key with wrong password") {
            val exception = shouldThrow<InvalidArgumentException> {
                createToken(TokenParams(
                        claims,
                        rsaAlgAndFile = Pair(SignatureAlgorithm.RS512, "/private_key_1.pem".resourceFile()),
                        passwordProvider = { "WRONG!!!".toCharArray() }
                ))
            }
            exception.message shouldBe "Invalid PEM file"
        }
    }
})
