package com.github.leomillon.jwt.generator

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.MissingRequiredPositionalArgumentException
import com.xenomachina.argparser.ShowHelpException
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.StringWriter

object ReadCmdSpec : Spek({

    fun parseArgs(args: Array<String>): ReadArgs {
        return ArgParser(args).parseInto(::ReadArgs)
    }

    describe("Help rendering") {
        it("should throw exception to display help") {
            shouldThrow<ShowHelpException> {
                parseArgs(arrayOf("--help"))
            }.run {
                message shouldBe "Help was requested"
                val help = StringWriter().apply { printUserMessage(this, "some_name", 0) }.toString()
                help shouldBe """
usage: some_name [-h] TOKEN [-s SECRET] [--standard] [--ignore-expiration] [--ignore-signature]

optional arguments:
  -h, --help                   show this help message and exit

  -s SECRET, --secret SECRET   signature base64 encoded secret key

  --standard, --json           output format (default: STANDARD

  --ignore-expiration          read the jwt claims/header even if the token is expired

  --ignore-signature           read the jwt claims/header even if the signature is invalid (displayed data cannot be trusted!!!)


positional arguments:
  TOKEN                        the JWT token to read

"""
                        .trimStart()
            }
        }
    }

    describe("Parse string args to command args") {
        it("No args should throw error and ask for token arg") {
            val exception = shouldThrow<MissingRequiredPositionalArgumentException> {
                parseArgs(emptyArray())
            }
            exception.message shouldBe "missing TOKEN operand"
        }

        it("should use token arg only") {
            val token = "SOME_TOKEN"
            val result = parseArgs(arrayOf(token))

            result.token shouldBe token
            result.secret shouldBe null
            result.format shouldBe OutputFormat.STANDARD
            result.ignoreExpiration shouldBe false
            result.ignoreSignature shouldBe false
        }

        it("should use all args") {
            val token = "SOME_TOKEN"
            val secret = "SOME_SECRET"
            val result = parseArgs(arrayOf(
                    token,
                    "--secret", secret,
                    "--json",
                    "--ignore-expiration",
                    "--ignore-signature"
            ))

            result.token shouldBe token
            result.secret shouldBe secret
            result.format shouldBe OutputFormat.JSON
            result.ignoreExpiration shouldBe true
            result.ignoreSignature shouldBe true
        }
    }
})
