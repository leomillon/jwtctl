package com.github.leomillon.jwt.generator

import ch.qos.logback.classic.Level
import io.kotlintest.matchers.endWith
import io.kotlintest.matchers.equalityMatcher
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.startWith
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.contrib.java.lang.system.ExpectedSystemExit
import org.junit.contrib.java.lang.system.SystemErrRule
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.runners.model.Statement

object IntegrationSpec : Spek({

    beforeEachTest { changeLogLevel(Level.WARN) }

    describe("Global commands") {
        it("Display help") {

            runCommandLine("--help") { out: String, err: String ->
                out shouldBe """
usage: jwtctl [-h] [-v] [--debug] [--version] COMMAND [ARGS]...

tool used to read or create JWT tokens. See more info at https://jwt.io/

optional arguments:
  -h, --help  show this help message and exit

  -v,         enable verbose mode
  --verbose

  --debug     enable debug mode

  --version   show program version and exit


positional arguments:
  COMMAND     the command to excecute : [create, read]

  ARGS        the command args


'jwtctl COMMAND --help' to read about a specific command
"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("Display version from config") {

            runCommandLine("--version") { out: String, err: String ->
                out shouldBe """
jwtctl version SOME_TEST_VERSION
"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("Enable verbose mode") {

            runCommandLine("--version", "--verbose") { out: String, err: String ->
                out shouldBe """
INFO  | Verbose mode enabled
jwtctl version SOME_TEST_VERSION
"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("Enable debug mode") {

            runCommandLine("--version", "--debug") { out: String, err: String ->
                out shouldBe """
DEBUG | Debug mode enabled
DEBUG | Input args = [--version, --debug]
jwtctl version SOME_TEST_VERSION
"""
                        .trimStart()

                err shouldBe empty()
            }
        }

        it("No args should ask for the command operand") {

            runCommandLine(expectedExitCode = 2) { out: String, err: String ->
                out shouldBe empty()
                err shouldBe haveLines(
                        equalityMatcher("jwtctl: missing COMMAND operand. See jwtctl --help"),
                        empty()
                )
            }
        }
    }

    describe("Create command") {

        val command = "create"

        it("Display help") {

            runCommandLine(command, "--help") { out: String, err: String ->
                out shouldBe """
usage: jwtctl create [-h] [-f CLAIMS_FILE] [-c NAME VALUE]... [--headers-file HEADERS_FILE]
                     [--header NAME VALUE]... [-d DURATION] [--deflate]
                     [--hmac-sign HMAC_ALG SECRET] [--rsa-sign RSA_ALG FILE_PATH] [-p PASSWORD]

optional arguments:
  -h, --help                     show this help message and exit

  -f CLAIMS_FILE,                JSON claims file path
  --claims-file CLAIMS_FILE

  -c NAME VALUE,                 claim to add to jwt body (override claims from file)
  --claim NAME VALUE

  --headers-file HEADERS_FILE    JSON headers file path

  --header NAME VALUE            header to add to jwt header (override headers from file)

  -d DURATION,                   set the duration of the token (expiration date = now +
  --duration DURATION            duration).
                                 Format : PTnHnMn.nS (ex: PT10H = 10 hours)

  --deflate, --gzip              the compression codec to use

  --hmac-sign HMAC_ALG SECRET    HMAC signature algorithm and base64 encoded secret key.
                                 Available
                                 algorithms : [HS256, HS384, HS512]

  --rsa-sign RSA_ALG FILE_PATH   RSA signature algorithm and Private Key (PEM) file path.
                                 Available
                                 algorithms : [RS256, RS384, RS512, PS256, PS384, PS512]

  -p PASSWORD,                   the password of the encrypted PEM file.
  --password PASSWORD            Will be asked interactively
                                 if not provided via this parameter.

"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("should create JWT token") {

            runCommandLine(command,
                    "--claims-file", "/claims.json".resourcePath(),
                    "--claim", "myClaim", "test",
                    "--headers-file", "/headers.json".resourcePath(),
                    "--header", "myHeader", "test",
                    "--duration", "PT2H30M",
                    "--deflate"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        jwtFormat(),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should create JWS token") {

            runCommandLine(command,
                    "--claims-file", "/claims.json".resourcePath(),
                    "--claim", "myClaim", "test",
                    "--headers-file", "/headers.json".resourcePath(),
                    "--header", "myHeader", "test",
                    "--duration", "PT2H30M",
                    "--hmac-sign", "HS512", "SOME_SECRET",
                    "--deflate"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        jwsFormat(),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should create RSA JWS token (RS256)") {

            runCommandLine("create",
                    "--claim", "myClaim", "test",
                    "--rsa-sign", "RS256", "/private_key_1.pem".resourcePath(),
                    "--password", "changeit"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        jwsFormat(),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should create RSA JWS token (PS256)") {

            runCommandLine("create",
                    "--claim", "myClaim", "test",
                    "--rsa-sign", "PS256", "/private_key_1.pem".resourcePath(),
                    "--password", "changeit"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        jwsFormat(),
                        empty()
                )
                err shouldBe empty()
            }
        }

        describe("Interactive input") {

            beforeGroup {
                System.setIn("changeit".byteInputStream())
            }

            it("should create RSA JWS token (RS256) without_pass") {

                runCommandLine("create",
                        "--claim", "myClaim", "test",
                        "--rsa-sign", "RS256", "/private_key_1.pem".resourcePath()
                ) { out: String, err: String ->
                    out shouldBe haveLines(
                            startWith("Enter password (").and(endWith("private_key_1.pem) : ")),
                            jwsFormat(),
                            empty()
                    )
                    err shouldBe empty()
                }
            }

            afterGroup {
                System.setIn(System.`in`)
            }
        }
    }

    describe("Read command") {

        val command = "read"

        it("Display help") {

            runCommandLine(command, "--help") { out: String, err: String ->
                out shouldBe """
usage: jwtctl read [-h] TOKEN [-s SECRET] [-f PUBLIC_KEY_FILE] [--standard] [--ignore-expiration]
                   [--ignore-signature]

optional arguments:
  -h, --help                          show this help message and exit

  -s SECRET, --secret SECRET          signature base64 encoded secret key

  -f PUBLIC_KEY_FILE,                 Public Key (PEM) file path
  --public-key-file PUBLIC_KEY_FILE

  --standard, --json                  output format (default: STANDARD)

  --ignore-expiration                 read the jwt claims/header even if the token is expired

  --ignore-signature                  read the jwt claims/header even if the signature is invalid
                                      (displayed data cannot be trusted!!!)


positional arguments:
  TOKEN                               the JWT token to read

"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("No args should ask for the token operand") {

            runCommandLine(command, expectedExitCode = 2) { out: String, err: String ->
                out shouldBe empty()
                err shouldBe haveLines(
                        equalityMatcher("jwtctl read: missing TOKEN operand. See jwtctl read --help"),
                        empty()
                )
            }
        }

        it("should read JWS token ignoring signature") {

            runCommandLine(command,
                    "eyJhbGciOiJIUzUxMiJ9" +
                            ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NzY0fQ" +
                            ".3SlJH4_hojObopvtrENgJ-TgqbuXvBH4tqXwiFbH7TVhg0ujx50UvvFThU5jQV_S4jyt9TXVYB-55xc9_Quhbw",
                    "--ignore-signature"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        equalityMatcher("WARN  | !!! Token signature has been ignored !!!"),
                        equalityMatcher("{sub=test-token, iat=1517438764}"),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should read JWS token (HS512)") {

            runCommandLine(command,
                    "eyJhbGciOiJIUzUxMiJ9" +
                            ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NzY0fQ" +
                            ".3SlJH4_hojObopvtrENgJ-TgqbuXvBH4tqXwiFbH7TVhg0ujx50UvvFThU5jQV_S4jyt9TXVYB-55xc9_Quhbw",
                    "--secret", "some_secret"

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        equalityMatcher("{sub=test-token, iat=1517438764}"),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should read JWS token (RS256)") {

            runCommandLine("read",
                    "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1MTg5NTE0MjQsIm15Q2xhaW0iOiJ0ZXN0In0.KC-sBb1Aec_04mKaN1x_2vKbFtgAbDXBe9ZH-MKddEhQBZsYqXqepjC-NU4MYvF2IpkC8vBXvhhrZycwG_fTHFcY1txWTwd0Q2Ix7vZ76jvX-Qn7UHlEsd11kPIxNs7HQA8LKr4250IGzUBpKHxdAPnYZ1GZ4K7VFGr_es1zVrzrl-zim2bze-ZwbPS9sMpZ2B_9KN7lRsow1jyxoXDkam8yZT182xAepfdYfGoVv06RcnPDFu-jp5pvRQFEoq5z0vbtji6aR8ZTUyfBmybabFiCgjFPy877grUaS5Rx9B_DEK7I6YbVHCTE6Q6CfQlxMqRHNIWFt0BNVzNEC7iriA",
                    "--public-key-file", "/public_key_1.pem".resourcePath()

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        equalityMatcher("{iat=1518951424, myClaim=test}"),
                        empty()
                )
                err shouldBe empty()
            }
        }

        it("should read RSA JWS token (PS256)") {

            runCommandLine("read",
                    "eyJhbGciOiJQUzI1NiJ9.eyJpYXQiOjE1MTg5NTI4NjYsIm15Q2xhaW0iOiJ0ZXN0In0.SaZTmZFbDEx5Y934AgdlpN4xu3Sh6Pg667SxUlF0hqeaG8olXJbPPBSoPW-J_W1ED3WQy_NQFtaRIE9f-DVz-bdQp9NAofW8p9235Lqo1GiDeKL-yDpKLKXFGLrlg8_1MZBjELTrttOgFlMhe1meJcTnEj0e7tezno2hF3H6NoV12s4fp1wmeaSYVsOAeAA_DZIq6c9GZuqWZM1u0m_8JzShiOfe0kiiRigGPoe8S-x_XIHw4s5lGl2PoNNMo8kHYs8Cnm_EyoL1icXxrlcrTN9C1reiytjqo-gK4tij9-6nYG7t8vGgri2owaa5LuQ0s0kG0Dl4-M7XQTG3b9ssMQ",
                    "--public-key-file", "/public_key_1.pem".resourcePath()

            ) { out: String, err: String ->
                out shouldBe haveLines(
                        equalityMatcher("{iat=1518952866, myClaim=test}"),
                        empty()
                )
                err shouldBe empty()
            }
        }
    }
})

private fun runCommandLine(vararg args: String, expectedExitCode: Int = 0, logAssertions: (outLog: String, errLog: String) -> Unit = { _: String, _: String -> }) {
    checkExitAndExtractLog({
        main(arrayOf(*args))
    }, expectedExitCode, logAssertions)
}

private fun checkExitAndExtractLog(workToDo: () -> Unit, expectedExitCode: Int, logAssertions: (outLog: String, errLog: String) -> Unit) {
    val statement: Statement = object : Statement() {
        override fun evaluate() {
            workToDo()
        }
    }
    val systemExit = ExpectedSystemExit.none()
    systemExit.expectSystemExitWithStatus(expectedExitCode)
    val systemOutRule = SystemOutRule()
    systemOutRule.enableLog()
    val systemErrRule = SystemErrRule()
    systemErrRule.enableLog()
    systemExit.checkAssertionAfterwards({
        logAssertions(systemOutRule.log, systemErrRule.log)
    })
    systemExit.apply(systemOutRule.apply(systemErrRule.apply(statement, null), null), null).evaluate()
    systemErrRule.clearLog()
    systemOutRule.clearLog()
}
