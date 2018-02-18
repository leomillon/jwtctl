package com.github.leomillon.jwt.generator

import com.xenomachina.argparser.InvalidArgumentException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.CompressionCodec
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.pkcs.RSAPublicKey
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMException
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider
import java.io.File
import java.io.FileReader
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPublicKeySpec

fun createToken(tokenParams: TokenParams): String {

    val jwtBuilder = Jwts.builder()
            .setClaims(tokenParams.claims)
            .setHeader(tokenParams.headers)

    tokenParams.hmacAlgAndSecret?.let { jwtBuilder.signWith(it.first, it.second) }

    tokenParams.rsaAlgAndFile?.let {
        try {
            jwtBuilder.signWith(it.first, pemFileToPrivateKey(it.second, tokenParams.passwordProvider))
        } catch (e: PEMException) {
            throw InvalidArgumentException("Invalid PEM file")
        }
    }

    tokenParams.compressionCodec?.let { jwtBuilder.compressWith(it) }

    return jwtBuilder.compact()
}

private fun pemFileToPrivateKey(pemFile: File, passwordProvider: (() -> CharArray)? = null): PrivateKey {
    val privateKeyInfo = toPemKeyPair(pemFile, passwordProvider)

    val rsaPrivateKey = RSAPrivateKey.getInstance(privateKeyInfo.parsePrivateKey())

    val rsaPrivateKeySpec = RSAPrivateKeySpec(rsaPrivateKey.modulus, rsaPrivateKey.privateExponent)
    return KeyFactory.getInstance("RSA").generatePrivate(rsaPrivateKeySpec)
}

private fun toPemKeyPair(pemFile: File, passwordProvider: (() -> CharArray)? = null): PrivateKeyInfo {
    val pemObject = PEMParser(FileReader(pemFile)).readObject()

    return when (pemObject) {
        is PEMEncryptedKeyPair -> {
            val password = passwordProvider?.invoke()
                    ?: throw InvalidArgumentException("Missing PEM password")
            pemObject.decryptKeyPair(BcPEMDecryptorProvider(password))
        }
        is PEMKeyPair -> pemObject
        else -> throw InvalidArgumentException("Invalid key-pair file")
    }.privateKeyInfo!!
}

private fun pemFileToPublicKey(pemFile: File): PublicKey {
    val publicKeyInfo = PEMParser(FileReader(pemFile))
            .readObject()
            as? SubjectPublicKeyInfo
            ?: throw InvalidArgumentException("Invalid public key file")

    val rsaPublicKey = RSAPublicKey.getInstance(publicKeyInfo.parsePublicKey())

    val rsaPublicKeySpec = RSAPublicKeySpec(rsaPublicKey.modulus, rsaPublicKey.publicExponent)
    return KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec)
}

data class TokenParams(
        val claims: Claims = Jwts.claims(),
        val headers: Header<out Header<*>> = Jwts.header(),
        val hmacAlgAndSecret: Pair<SignatureAlgorithm, String>? = null,
        val rsaAlgAndFile: Pair<SignatureAlgorithm, File>? = null,
        val passwordProvider: (() -> CharArray)? = null,
        val compressionCodec: CompressionCodec? = null
)

fun readToken(
        token: String,
        base64EncodedSecret: String? = null,
        pemFile: File? = null,
        ignoreExpiration: Boolean = false,
        ignoreSignature: Boolean = false): ParsedToken {
    val parser = Jwts.parser()

    val signatureIgnored = parser.isSigned(token) && ignoreSignature
    try {
        return if (signatureIgnored) {
            val tokenParts = token.split(".")
            val jwt = parser.parse("${tokenParts[0]}.${tokenParts[1]}.")
            ParsedToken(jwt.header, jwt.body, ignoredSignature = signatureIgnored)
        } else {
            base64EncodedSecret?.let { parser.setSigningKey(it) }
            pemFile?.let { pemFileToPublicKey(it) }?.let { parser.setSigningKey(it) }
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
