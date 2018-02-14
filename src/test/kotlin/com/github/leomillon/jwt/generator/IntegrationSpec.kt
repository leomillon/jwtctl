package com.github.leomillon.jwt.generator

import ch.qos.logback.classic.Level
import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.match
import io.kotlintest.matchers.shouldBe
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
                err shouldBe """
jwtctl: missing COMMAND operand. See jwtctl --help
"""
                        .trimStart()
            }
        }
    }

    describe("Create command") {

        val command = "create"

        it("Display help") {

            runCommandLine(command, "--help") { out: String, err: String ->
                out shouldBe """
usage: jwtctl create [-h] [-f CLAIMS_FILE] [-c NAME VALUE] [--headers-file HEADERS_FILE]
                     [--header NAME VALUE] [-d DURATION] [--deflate] [-s ALGORITHM SECRET]

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

  -s ALGORITHM SECRET,           signature algorithm and base64 encoded secret key. Available
  --signature ALGORITHM SECRET   algorithm : [HS256, HS384, HS512]

"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("should create token") {

            runCommandLine(command,
                    "--claims-file", "/claims.json".resourcePath(),
                    "--claim", "myClaim", "test",
                    "--headers-file", "/headers.json".resourcePath(),
                    "--header", "myHeader", "test",
                    "--duration", "PT2H30M",
                    "--signature", "HS512", "SOME_SECRET",
                    "--deflate"

            ) { out: String, err: String ->
                out shouldBe match("""^[\w-_]+.[\w-_]+.[\w-_]+\n$""")
                err shouldBe empty()
            }
        }
    }

    describe("Read command") {

        val command = "read"

        it("Display help") {

            runCommandLine(command, "--help") { out: String, err: String ->
                out shouldBe """
usage: jwtctl read [-h] TOKEN [-s SECRET] [--standard] [--ignore-expiration] [--ignore-signature]

optional arguments:
  -h, --help            show this help message and exit

  -s SECRET,            signature base64 encoded secret key
  --secret SECRET

  --standard, --json    output format (default: STANDARD

  --ignore-expiration   read the jwt claims/header even if the token is expired

  --ignore-signature    read the jwt claims/header even if the signature is invalid (displayed
                        data cannot be trusted!!!)


positional arguments:
  TOKEN                 the JWT token to read

"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("should read token") {

            runCommandLine(command,
                    "eyJhbGciOiJIUzUxMiJ9" +
                            ".eyJzdWIiOiJ0ZXN0LXRva2VuIiwiaWF0IjoxNTE3NDM4NzY0fQ" +
                            ".3SlJH4_hojObopvtrENgJ-TgqbuXvBH4tqXwiFbH7TVhg0ujx50UvvFThU5jQV_S4jyt9TXVYB-55xc9_Quhbw",
                    "--ignore-signature"

            ) { out: String, err: String ->
                out shouldBe """
WARN  | !!! Token signature has been ignored !!!
{sub=test-token, iat=1517438764}
"""
                        .trimStart()
                err shouldBe empty()
            }
        }

        it("No args should ask for the token operand") {

            runCommandLine(command, expectedExitCode = 2) { out: String, err: String ->
                out shouldBe empty()
                err shouldBe """
jwtctl read: missing TOKEN operand. See jwtctl read --help
"""
                        .trimStart()
            }
        }
    }
})

fun empty(): Matcher<String> = match("")

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
