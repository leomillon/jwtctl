package com.github.leomillon.jwt.generator

import io.kotlintest.matchers.Matcher
import io.kotlintest.matchers.Result
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
