package com.github.leomillon.jwt.generator

import com.xenomachina.argparser.InvalidArgumentException
import io.jsonwebtoken.*

fun createToken(tokenParams: TokenParams): String {

    val jwtBuilder = Jwts.builder()
            .setClaims(tokenParams.claims)
            .setHeader(tokenParams.headers)

    tokenParams.signatureAlgAndSecret?.let { jwtBuilder.signWith(it.first, it.second) }

    tokenParams.compressionCodec?.let { jwtBuilder.compressWith(it) }

    return jwtBuilder.compact()
}

data class TokenParams(
        val claims: Claims = Jwts.claims(),
        val headers: Header<out Header<*>> = Jwts.header(),
        val signatureAlgAndSecret: Pair<SignatureAlgorithm, String>? = null,
        val compressionCodec: CompressionCodec? = null
)

fun readToken(token: String, base64EncodedSecret: String? = null, ignoreExpiration: Boolean = false, ignoreSignature: Boolean = false): ParsedToken {
    val parser = Jwts.parser()

    val signatureIgnored = parser.isSigned(token) && ignoreSignature
    try {
        return if (signatureIgnored) {
            val tokenParts = token.split(".")
            val jwt = parser.parse("${tokenParts[0]}.${tokenParts[1]}.")
            ParsedToken(jwt.header, jwt.body, ignoredSignature = signatureIgnored)
        }
        else {
            base64EncodedSecret?.let { parser.setSigningKey(it) }
            val jwt = parser.parse(token)
            ParsedToken(jwt.header, jwt.body)
        }
    } catch (e: ExpiredJwtException) {
        if (ignoreExpiration) {
            return ParsedToken(e.header, e.claims, expired = true, ignoredSignature = signatureIgnored)
        }
        throw InvalidArgumentException("${e.message}")
    }
}

data class ParsedToken(
        val header: Header<*>,
        val body: Any,
        val expired: Boolean = false,
        val ignoredSignature: Boolean = false)
