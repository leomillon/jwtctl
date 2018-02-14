package com.github.leomillon.jwt.generator

import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.SystemExitException

fun <T> wrapCommandBody(commandName: String? = null, body: () -> T) {

    var commandPrefix = Application.name
    commandName?.let { commandPrefix += " $it" }

    try {
        body()
    } catch (e: Exception) {
        // Avoid test CheckExitCalled catch
        if (e is SecurityException) {
            throw e
        }
        // Used for help by arg parser library
        if (e is ShowHelpException) {
            e.printAndExit(programName = commandPrefix, columns = getColumnWidth())
        }
        val exitCode = when (e) {
            is SystemExitException -> e.returnCode
            else -> 9999
        }
        if (log.isDebugEnabled) {
            e.printStackTrace()
        }
        displayAndExit("$commandPrefix: ${e.message}. See $commandPrefix --help", exitCode)
    }
}

private fun getColumnWidth(): Int {
    val config = systemProperties() overriding EnvironmentVariables()
    return config.getOrElse(Key("COLUMNS", intType), 100)
}
