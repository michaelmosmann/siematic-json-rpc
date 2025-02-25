package de.flapdoodle.siematic

import de.flapdoodle.net.Net
import de.flapdoodle.siematic.api.ApiLogin
import de.flapdoodle.siematic.api.ApiLoginResponse
import de.flapdoodle.siematic.api.ArrayDimensions
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.ReadMethodCallResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

object Main {

  inline fun <reified T> request(json: T, vararg header: Pair<String, String>): HttpRequest {
    var httpRequestBuilder = HttpRequest.newBuilder(URI("https://siemens.fritz.box/api/jsonrpc"))
      .timeout(Duration.of(10, SECONDS))
      .header("Content-Type", "application/json")

    header.forEach {
      httpRequestBuilder = httpRequestBuilder.header(it.first, it.second)
    }

    val request = httpRequestBuilder
      .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(json)))
      .build()

    return request
  }

  fun responseBody(request: HttpRequest): String {
    val response = HttpClient.newBuilder()
      //    .proxy(ProxySelector.getDefault())
      .sslContext(Net.acceptAllSSLContext())
      .build()
      .send(request, BodyHandlers.ofString())

    require(response.statusCode() == 200) {
      "request failed: $request, status: ${response.statusCode()}"
    }

    return response.body()
  }

  inline fun <reified T> response(request: HttpRequest): T {
    val body = responseBody(request)
    try {
      return Json.decodeFromString<T>(body)
    } catch (e: Exception) {
      throw RuntimeException("failed to parse response:\n${body}\n", e)
    }
  }

  fun readValue(token: String, name: String, datatype: String): Any {
    val request = request(MethodCall.read("id", name),"x-auth-token" to token)
    return when (datatype) {
      "real" -> response<ReadMethodCallResponse.ReadNumber>(request).result
      "bool" -> response<ReadMethodCallResponse.ReadBoolean>(request).result
      "usint" -> response<ReadMethodCallResponse.ReadNumber>(request).result

      else -> throw IllegalArgumentException("unknown datatype: $datatype")
    }
  }

  fun readValues(token: String, values: List<Pair<String, String>>): Map<String, Any> {
    if (values.isNotEmpty()) {
      val valueTypeMap = values.toMap()

      val requestBody = values.map { MethodCall.read(it.first, it.first) }
      val request = request(requestBody, "x-auth-token" to token)
      val json = responseBody(request)

      val parsed = Json.parseToJsonElement(json)
      require(parsed is JsonArray) { "wrong type: $parsed - $requestBody" }
      return parsed.jsonArray.associate {
        val id = it.jsonObject.getValue("id").jsonPrimitive.content
        val keys = it.jsonObject.keys
        require(keys.contains("result")) { "missing 'result' in $keys ($it, $requestBody)"}
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

  fun browse(token: String, name: String, level: Int = 0) {
    println("${"  ".repeat(level)}${name}")

    val browseRequest = request(MethodCall.browse("id", name),"x-auth-token" to token)
    val browseResult = response<BrowseMethodCallResponse>(browseRequest)

    val withChildren = browseResult.result.filter { it.has_children }
    val values = browseResult.result.filter { !it.has_children }

    withChildren.forEach {
      when (it.datatype) {
        "struct" -> {
          val arrayDimensions = it.array_dimensions
          if (arrayDimensions!=null) {
            val offsets = unroll(arrayDimensions)
            offsets.forEach { offset ->
              browse(token, "$name.${it.name}[$offset]", level + 1)
            }
          } else {
            browse(token, "$name.${it.name}", level + 1)
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
    val valueMap = readValues(token, readValuesJson)
    valueMap.forEach {
      println("${"  ".repeat(level + 1)}$name.${it.key} = ${it.value}")
    }
  }

  @JvmStatic
  fun main(vararg args: String) {
    val request = request(ApiLogin.loginWith("Service", "2010"))
    val tokenResponse = response<ApiLoginResponse>(request)
    val token = tokenResponse.result.token
    println("token: ${token}")

    browse(token, "\"dbTank\"")
  }
}

