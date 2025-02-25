package de.flapdoodle.siematic

import de.flapdoodle.siematic.api.ApiLogin
import de.flapdoodle.siematic.api.ApiLoginResponse
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.PropertyResponse

object Main {

  fun browse(jsonRpc: JsonRpc, current: String, properties: List<PropertyResponse>, level: Int, maxDepth: Int) {
    val indent = "  ".repeat(level)
    println("$indent$current")
    
    if (level + 1 <= maxDepth) {
      val (withChildren, withoutChildren) = properties.partition { it.has_children }

      try {
        jsonRpc.readProperties(current, withoutChildren)
      } catch (ex: RuntimeException) {
        throw RuntimeException("could not call: $withoutChildren", ex)
      }.forEach { (key, value) ->
        println("$indent  ${key} = $value")
      }

      val properties = withChildren.flatMap {
        it.childNames(current)
      }

      val browseChildCalls = properties.map {
        MethodCall.browse(it, it)
      }

      try {
        jsonRpc.browseMethodCalls(browseChildCalls)
      } catch (ex: RuntimeException) {
        throw RuntimeException("could not call: $withChildren", ex)
      }.forEach {
        if (it.error != null) {
          println("$indent$current - ${it.id} failed with error: ${it.error}")
        } else {
          browse(jsonRpc, it.id, it.result, level + 1, maxDepth)
        }
      }
    }
  }

  fun browse(jsonRpc: JsonRpc, current: String, level: Int) {
    val escapedName = "\"$current\""
    val response = jsonRpc.response<BrowseMethodCallResponse>(jsonRpc.request(MethodCall.browse(escapedName, escapedName)))
    browse(jsonRpc, escapedName, response.result, level, Int.MAX_VALUE)
  }

  fun browse(jsonRpc: JsonRpc, maxDepth: Int) {
    val all = jsonRpc.browseAll()
    browse(jsonRpc, "", all, 0, maxDepth)
  }

  @JvmStatic
  fun main(vararg args: String) {
    val jsonRpc = JsonRpc()
    val request = jsonRpc.request(ApiLogin.loginWith("Service", "2010"))
    val tokenResponse = jsonRpc.response<ApiLoginResponse>(request)
    val token = tokenResponse.result.token
    println("token: ${token}")
    val jsonRpcWithAuth = jsonRpc.withHeader("x-auth-token" to token)

    val browseAll = false

    if (browseAll) {
      browse(jsonRpcWithAuth, Int.MAX_VALUE)
    } else {
      val sections = listOf(
//        "configurationSettings",
//        "customSettings",
//        "technicalSettings",
//        "screedSettings",
        "operatingData",
//        "dbService",
//        "dbLoadSave",
//        "DB 333",
//        "dbAI",
//        "dbAQ",
//        "dbDI",
//        "dbDQ",
//        "dbPTO",
//        "dbMaster",
//        "dbMaster2",
//        "dbAlternate2",
//        "dbAlternate3",
//        "dbOuterTemp",
//        "dbMode",
//        "dbSGReady",
//        "dbVacation",
//        "dbRoom",
//        "dbFloorpump",
//        "dbBypass",
//        "dbTank",
//        "dbCirculation",
//        "dbExchanger",
//        "dbDewpoint",
//        "dbMixer",
//        "dbHeatpump",
//        "dbScreed",
//        "dbHeatpumpBeglau",
//        "dbHeatpumpMitsu",
//        "dbConverter",
//        "dbInjector",
//        "dbFan",
//        "dbDefrost",
//        "dbAlarm",
//        "dbAlarms",
//        "dbWEB_Data",
//        "dbClock",
//        "dbDateTime",
//        "dbConfigIP",
//        "dbConfigCDN",
//        "dbTimerDayNight",
//        "PTO"
      )

      sections.forEach { section ->
        browse(jsonRpcWithAuth, section, 1)
      }
    }
  }
}

