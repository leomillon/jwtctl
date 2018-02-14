package com.github.leomillon.jwt.generator

import com.xenomachina.argparser.InvalidArgumentException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
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
        val jws = "eyJhbGciOiJIUzUxMiJ9" +
                ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NzY0fQ" +
                ".3SlJH4_hojObopvtrENgJ-TgqbuXvBH4tqXwiFbH7TVhg0ujx50UvvFThU5jQV_S4jyt9TXVYB-55xc9_Quhbw"
        val expiredJws = "eyJhbGciOiJIUzUxMiJ9" +
                ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5MTAyLCJleHAiOjE1MTc0MzkxMDJ9" +
                ".PNoZ4DS-OipT3UO6LJmr9TU4MBH8xbUEMR99Fxhwu616otq9y-07KdpeiTijL1hfJCtS2CfNPkWhkGzYu-ESiQ"
        val compressedSignedJws = "eyJhbGciOiJIUzUxMiIsInppcCI6IkRFRiJ9" +
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
            exception.message shouldBe startWith("JWT expired at 2018-01-31T23:59:49Z.")
        }

        it("should read JWS token") {
            val result = readToken(jws, secret)

            result.header.toString() shouldBe "{alg=HS512}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438764}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should not read expired JWS token") {
            val exception = shouldThrow<InvalidArgumentException> {
                readToken(expiredJws, secret)
            }
            exception.message shouldBe startWith("JWT expired at 2018-01-31T23:51:42Z")
        }

        it("should not read JWTS token without secret") {
            val exception = shouldThrow<IllegalArgumentException> {
                readToken(jws)
            }
            exception.message shouldBe startWith("A signing key must be specified if the specified JWT is digitally signed.")
        }

        it("should read compressed JWS token") {
            val result = readToken(compressedSignedJws, secret)

            result.header.toString() shouldBe "{alg=HS512, zip=DEF}"
            result.body.toString() shouldBe "{sub=test-token, iat=1517438874}"
            result.expired shouldBe false
            result.ignoredSignature shouldBe false
        }

        it("should read JWS ignoring signature") {
            val result = readToken(compressedSignedJws, ignoreSignature = true)

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
            val result = readToken(expiredJws, ignoreSignature = true, ignoreExpiration = true)

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

        it("shoud not create empty jwt") {
            val exception = shouldThrow<IllegalStateException> {
                createToken(TokenParams())
            }
            exception.message shouldBe startWith("Either 'payload' or 'claims' must be specified")
        }

        it("should create jwt") {
            val result = createToken(TokenParams(claims))

            result shouldBe "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ."
        }

        it("should create jws") {
            val result = createToken(TokenParams(
                    claims,
                    signatureAlgAndSecret = Pair(SignatureAlgorithm.HS512, "some_secret")
            ))

            result shouldBe "eyJhbGciOiJIUzUxMiJ9" +
                    ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM5NTg5fQ" +
                    ".uPsQwXp_s4iVZOE56frIZE8lx-zNGT8peZ4kT6yO1yeUnzJFZ7d47VSkSGjZvohha2v_UZ-bbicQw0J2ir6jNg"
        }
    }
})
