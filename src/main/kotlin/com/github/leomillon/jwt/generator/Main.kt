package com.github.leomillon.jwt.generator

import ch.qos.logback.classic.Level
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) = wrapCommandBody {

    val rawArgs = RawArgs()
    val commandArgs = args.toList().filter {
        when (it) {
            "-v", "--verbose" -> {
                rawArgs.verbose = true
                false
            }
            "--debug" -> {
                rawArgs.debug = true
                false
            }
            "--version" -> {
                rawArgs.version = true
                false
            }
            else -> {
                if (rawArgs.command == null && Command.codes().contains(it)) {
                    rawArgs.command = Command.fromCode(it)
                    false
                } else {
                    true
                }
            }
        }
    }
            .toTypedArray()

    if (rawArgs.debug) {
        changeLogLevel(Level.DEBUG)
        log.debug { "Debug mode enabled" }
    } else if (rawArgs.verbose) {
        changeLogLevel(Level.INFO)
        log.info { "Verbose mode enabled" }
    }

    log.debug { "Input args = ${args.toList()}" }

    if (rawArgs.version) {
        displayAndExit("${Application.name} version ${Application.config[project.version]}")
    }

    log.debug { "Command ${rawArgs.command} args = ${commandArgs.toList()}" }

    when (rawArgs.command) {
        Command.CREATE -> {
            execGenerate(commandArgs)
        }
        Command.READ -> {
            execRead(commandArgs)
        }
        else -> {
            ArgParser(
                    commandArgs,
                    helpFormatter = DefaultHelpFormatter(
                            prologue = "tool used to read or create JWT tokens. See more info at https://jwt.io/",
                            epilogue = "'${Application.name} COMMAND --help' to read about a specific command"
                    )
            )
                    .parseInto(::MainArgs)
        }
    }
}

class RawArgs {
    var verbose = false
    var debug = false
    var version = false
    var command: Command? = null
}

/**
 * Used for help display only
 */
class MainArgs(parser: ArgParser) {

    val verbose by parser.flagging("-v", "--verbose",
            help = "enable verbose mode")

    val debug by parser.flagging("--debug",
            help = "enable debug mode")

    val version by parser.flagging("--version",
            help = "show program version and exit")

    val command by parser.positional("COMMAND",
            help = "the command to excecute : ${Command.codes()}") {
        Command.fromCode(this)
                ?: throw InvalidArgumentException("'$this' is not a valid command. Must be one of theses ${Command.codes()}")
    }

    val commandArgs by parser.positionalList("ARGS",
            help = "the command args")
            .default(listOf())
}

enum class Command(val code: String) {
    CREATE("create"),
    READ("read");

    companion object {
        fun codes(): List<String> {
            return values().map { it.code }
        }

        fun fromCode(code: String): Command? {
            return values().find { it.code == code }
        }
    }
}

fun displayAndExit(message: String, exitCode: Int = 0) {
    val writer = OutputStreamWriter(if (exitCode == 0) System.out else System.err)
    writer.write("$message\n")
    writer.flush()
    exitProcess(exitCode)
}
