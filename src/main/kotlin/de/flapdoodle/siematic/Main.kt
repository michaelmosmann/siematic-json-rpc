package de.flapdoodle.siematic

import de.flapdoodle.siematic.api.ApiLogin
import de.flapdoodle.siematic.api.ApiLoginResponse
import de.flapdoodle.siematic.api.ArrayDimensions
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.PropertyResponse
import de.flapdoodle.siematic.api.ReadMethodCallResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Main {

  fun readValue(jsonRpc: JsonRpc, name: String, datatype: String): Any {
    val request = jsonRpc.request(MethodCall.read("id", name))
    return when (datatype) {
      "real" -> jsonRpc.response<ReadMethodCallResponse.ReadNumber>(request).result
      "bool" -> jsonRpc.response<ReadMethodCallResponse.ReadBoolean>(request).result
      "usint" -> jsonRpc.response<ReadMethodCallResponse.ReadNumber>(request).result

      else -> throw IllegalArgumentException("unknown datatype: $datatype")
    }
  }

  fun readValues(jsonRpc: JsonRpc, values: List<Pair<String, String>>): Map<String, Any> {
    if (values.isNotEmpty()) {
      val valueTypeMap = values.toMap()

      val requestBody = values.map { MethodCall.read(it.first, it.first) }
      val request = jsonRpc.request(requestBody)
      val json = jsonRpc.responseBody(request)

      val parsed = Json.parseToJsonElement(json)
      require(parsed is JsonArray) { "wrong type: $parsed - $requestBody" }
      return parsed.jsonArray.associate {
        val id = it.jsonObject.getValue("id").jsonPrimitive.content
        val keys = it.jsonObject.keys
        require(keys.contains("result")) { "missing 'result' in $keys ($it, $requestBody)" }
        val jsonValue: JsonElement = it.jsonObject.getValue("result")
        val valueType = valueTypeMap[id]
        val value = when (valueType) {
          "bool" -> jsonValue.jsonPrimitive.boolean
          "real" -> jsonValue.jsonPrimitive.double
          "usint" -> jsonValue.jsonPrimitive.double
          "dint" -> jsonValue.jsonPrimitive.double
          else -> throw IllegalArgumentException("unknown datatype: $valueType ($jsonValue)")
        }

        id to value
      }
    }
    return emptyMap()
  }

  fun offset(arrayDimension: ArrayDimensions): List<Int> {
    return (arrayDimension.start_index..<(arrayDimension.start_index + arrayDimension.count)).toList()
  }

  fun unroll(arrayDimensions: List<ArrayDimensions>, index: Int, prefix: String): List<String> {
    val current = arrayDimensions[index]
    return offset(current).flatMap { i ->
      if (arrayDimensions.size > index + 1) {
        unroll(arrayDimensions, index + 1, "$prefix$i,")
      } else {
        listOf("$prefix$i")
      }
    }
  }

  fun unroll(arrayDimensions: List<ArrayDimensions>): List<String> {
    return unroll(arrayDimensions, 0, "")
  }

  fun browse(jsonRpc: JsonRpc, name: String, level: Int = 0) {
    println("${"  ".repeat(level)}${name}")

    val browseRequest = jsonRpc.request(MethodCall.browse("id", name))
    val browseResult = jsonRpc.response<BrowseMethodCallResponse>(browseRequest)

    val withChildren = browseResult.result.filter { it.has_children }
    val values = browseResult.result.filter { !it.has_children }

    withChildren.forEach {
      when (it.datatype) {
        "struct" -> {
          val arrayDimensions = it.array_dimensions
          if (arrayDimensions != null) {
            val offsets = unroll(arrayDimensions)
            offsets.forEach { offset ->
              browse(jsonRpc, "$name.${it.name}[$offset]", level + 1)
            }
          } else {
            browse(jsonRpc, "$name.${it.name}", level + 1)
          }
        }
      }
//      } else {
//        val value = readValue(token, "$name.${it.name}", it.datatype)
//        println("${"  ".repeat(level + 1)}$name.${it.name} = $value (${it.datatype})")
//      }
    }

    val readValuesJson = values.flatMap {
      val arrayDimensions = it.array_dimensions
      if (arrayDimensions != null) {
        val offsets = unroll(arrayDimensions)
        offsets.map { offset ->
          "$name.${it.name}[$offset]" to it.datatype
        }
      } else {
        listOf("$name.${it.name}" to it.datatype)
      }
    }
    val valueMap = readValues(jsonRpc, readValuesJson)
    valueMap.forEach {
      println("${"  ".repeat(level + 1)}$name.${it.key} = ${it.value}")
    }
  }

  val blacklist = setOf<String>(
//    "configurationSettings",
//    "customSettings",
//    "technicalSettings",
//    "screedSettings",
//    "operatingData",
//    "dbService",
//    "dbLoadSave",
//    "DB 333",
//    "dbAI",
//    "dbAQ",
//    "dbDI"
  )

  val whiteList = setOf(
    "\"PTO\""
  )

  private fun blacklisted(name: String): Boolean {
    return blacklist.contains(name)
  }

  fun browse(jsonRpc: JsonRpc, current: String, properties: List<PropertyResponse>, level: Int) {
    val indent = "  ".repeat(level)
    println("$indent$current")

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
        browse(jsonRpc, it.id, it.result, level + 1)
      }
    }

  }


  fun browse(jsonRpc: JsonRpc) {
    val all = jsonRpc.browseAll()
    browse(jsonRpc, "", all, 0)
  }

  @JvmStatic
  fun main(vararg args: String) {
    val jsonRpc = JsonRpc()
    val request = jsonRpc.request(ApiLogin.loginWith("Service", "2010"))
    val tokenResponse = jsonRpc.response<ApiLoginResponse>(request)
    val token = tokenResponse.result.token
    println("token: ${token}")
    val jsonRpcWithAuth = jsonRpc.withHeader("x-auth-token" to token)

    val new=true
    if (new) {
      browse(jsonRpcWithAuth)
    } else {
      browse(jsonRpcWithAuth, "\"dbTank\"")
    }
  }
}

