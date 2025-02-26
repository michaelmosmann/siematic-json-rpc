package de.flapdoodle.siematic

import de.flapdoodle.siematic.api.ApiLogin
import de.flapdoodle.siematic.api.ApiLoginResponse
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.PropertyResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object Main {

  fun browse(jsonRpc: JsonRpc, current: String, properties: List<PropertyResponse>, level: Int, maxDepth: Int, output: Output) {
    val indent = "  ".repeat(level)
    output.println("$indent$current")
    
    if (level + 1 <= maxDepth) {
      val (withChildren, withoutChildren) = properties.partition { it.has_children }

      try {
        jsonRpc.readProperties(current, withoutChildren)
      } catch (ex: RuntimeException) {
        throw RuntimeException("could not call: $withoutChildren", ex)
      }.forEach { (key, value) ->
        output.println("$indent  ${key} = $value")
      }

      val properties = withChildren.flatMap {
        it.childNames(current)
      }

      val browseChildCalls = properties.map {
        MethodCall.browse(it, it)
      }

      val browseResponses = try {
        jsonRpc.browseMethodCalls(browseChildCalls)
      } catch (ex: RuntimeException) {
        throw RuntimeException("could not call: $withChildren", ex)
      }

      val (withoutErrors, withErrors) = browseResponses.partition { it.error == null }

      withErrors.forEach {
        if (it.error != null) {
          output.println("$indent$current - ${it.id} failed with error: ${it.error}")
        }
      }

      withoutErrors.forEach {
        browse(jsonRpc, it.id, it.result, level + 1, maxDepth, output)
      }
    }
  }

  fun browse(jsonRpc: JsonRpc, current: String, level: Int, output: Output) {
    val escapedName = "\"$current\""
    val response = jsonRpc.response<BrowseMethodCallResponse>(jsonRpc.request(MethodCall.browse(escapedName, escapedName)))
    browse(jsonRpc, escapedName, response.result, level, Int.MAX_VALUE, output)
  }

  fun browse(jsonRpc: JsonRpc, maxDepth: Int, output: Output) {
    val all = jsonRpc.browseAll()
    browse(jsonRpc, "", all, 0, maxDepth, output)
  }

  fun interface Output {
    fun println(message: String)
  }

  @JvmStatic
  fun main(vararg args: String) {
    val basePath = Paths.get(System.getProperty("user.dir"))
    println("basePath: $basePath")

    val jsonRpc = JsonRpc()
    val request = jsonRpc.request(ApiLogin.loginWith("Service", "2010"))
    val tokenResponse = jsonRpc.response<ApiLoginResponse>(request)
    val token = tokenResponse.result.token
    println("token: $token")
    val jsonRpcWithAuth = jsonRpc.withHeader("x-auth-token" to token)

    val browseAll = false
    val output = { message: String ->
      println(message)
    }

    if (browseAll) {
      browse(jsonRpcWithAuth, Int.MAX_VALUE, output)
    } else {
      val sections = listOf(
        "configurationSettings",
//        "customSettings",
//        "technicalSettings",
        "screedSettings",
//        "operatingData",
        "dbService",
        "dbLoadSave",
        "DB 333",
        "dbAI",
        "dbAQ",
        "dbDI",
        "dbDQ",
        "dbPTO",
        "dbMaster",
        "dbMaster2",
        "dbAlternate2",
        "dbAlternate3",
        "dbOuterTemp",
        "dbMode",
        "dbSGReady",
        "dbVacation",
        "dbRoom",
        "dbFloorpump",
        "dbBypass",
        "dbTank",
        "dbCirculation",
        "dbExchanger",
        "dbDewpoint",
        "dbMixer",
        "dbHeatpump",
        "dbScreed",
        "dbHeatpumpBeglau",
        "dbHeatpumpMitsu",
        "dbConverter",
        "dbInjector",
        "dbFan",
        "dbDefrost",
        "dbAlarm",
        "dbAlarms",
        "dbWEB_Data",
        "dbClock",
        "dbDateTime",
        "dbConfigIP",
        "dbConfigCDN",
        "dbTimerDayNight",
        "PTO"
      )

      sections.forEach { section ->
        val dumpPath = basePath.resolve("http").resolve("$section.txt")
        if (!Files.exists(dumpPath)) {
          var stringBuilder = StringBuilder()
          val collectingOutput = Output { message: String ->
            output(message)
            stringBuilder.append(message).append("\n")
          }

          browse(jsonRpcWithAuth, section, 1, collectingOutput)

          Files.writeString(dumpPath, stringBuilder.toString(), Charsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
          output("$section DONE")
        } else {
          output("$section SKIP")
        }
      }
    }
  }
}

