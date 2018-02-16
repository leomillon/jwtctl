package com.github.leomillon.jwt.generator

import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Result
import io.kotlintest.matchers.match
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Date

object Utils {

    fun loadResource(path: String) = Utils::class.java.getResource(path)!!

    fun closeTo(expected: Instant): Matcher<Date> = object : Matcher<Date> {
        override fun test(value: Date): Result {
            val gapValue: Long = 1
            val timeGap = Duration.ofSeconds(gapValue)
            val valueAsInstant = value.toInstant()
            return Result(
                    valueAsInstant in expected.minus(timeGap)..expected.plus(timeGap),
                    "$valueAsInstant is close to ($expected +/- $gapValue sec)"
            )
        }
    }
}

fun String.resourcePath() = Utils.loadResource(this).path!!

fun String.resourceFile() = File(resourcePath())

fun empty(): Matcher<String> = match("")

fun jwtFormat(): Matcher<String> = match("""^[\w-_]+.[\w-_]+.$""")

fun jwsFormat(): Matcher<String> = match("""^[\w-_]+.[\w-_]+.[\w-_]+$""")

fun haveLines(vararg lineMatchers: Matcher<String>): Matcher<String> {
    return object : Matcher<String> {
        override fun test(value: String): Result {
            val lines = value.split("\n")
            if (lines.size != lineMatchers.size) {
                return Result(false, "${lines.size} lines for ${lineMatchers.size} matchers")
            }

            return lines
                    .mapIndexed { index, line -> lineMatchers[index].test(line) }
                    .firstOrNull { !it.passed }
                    ?: Result(true, "")
        }
    }
}
