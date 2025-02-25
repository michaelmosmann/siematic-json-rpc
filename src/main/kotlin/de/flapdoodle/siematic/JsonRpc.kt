package de.flapdoodle.siematic

import de.flapdoodle.net.Net
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.PropertyResponse
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

data class JsonRpc(
  val api: String = "https://siemens.fritz.box/api/jsonrpc",
  val header: List<Pair<String, String>> = emptyList()
) {

  fun withHeader(vararg headerPair: Pair<String, String>): JsonRpc {
    return copy(header = header + listOf(*headerPair))
  }

  inline fun <reified T> request(json: T): HttpRequest {
    var httpRequestBuilder = HttpRequest.newBuilder(URI(api))
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

  fun browseAll(): List<PropertyResponse> {
    val request = request(MethodCall.browseAll("all"))
    val response = response<BrowseMethodCallResponse>(request)
    return response.result
  }

  fun browseMethodCalls(calls: List<MethodCall>, maxCalls: Int): List<BrowseMethodCallResponse> {
//    return calls.map { call ->
//      val request = request(call)
//      try {
//        response<BrowseMethodCallResponse>(request)
//      } catch (ex: IllegalArgumentException) {
//        throw RuntimeException("could not call: $call", ex)
//      }
//    }
    return calls.chunked(maxCalls).flatMap { chunk ->
//      println("call $chunk -> ${Json.encodeToString(chunk)}")
      val request = request(chunk)
      try {
        response<List<BrowseMethodCallResponse>>(request)
      } catch (ex: IllegalArgumentException) {
        throw RuntimeException("could not call: $chunk", ex)
      }
    }
  }

  fun browseMethodCalls(calls: List<MethodCall>): List<BrowseMethodCallResponse> {
    return browseMethodCalls(calls, 100)
  }

  fun readProperties(current: String, properties: List<PropertyResponse>): List<Pair<String, Any>> {
    if (!properties.isEmpty()) {
      val nameAndType = properties.flatMap { p ->
        p.childNames(current).map { it to p.datatype }
      }
      val nameTypeMap = nameAndType.toMap()

      val methodCalls = nameAndType.map { MethodCall.read(it.first, it.first) }
      val request = request(methodCalls)
      val json = responseBody(request)

      val parsed = Json.parseToJsonElement(json)
      require(parsed is JsonArray) { "wrong type: $parsed - $methodCalls" }

      return parsed.jsonArray.map {
        val id = it.jsonObject.getValue("id").jsonPrimitive.content
        val keys = it.jsonObject.keys
        require(keys.contains("result")) { "missing 'result' in $keys ($it)" }
        val jsonValue: JsonElement = it.jsonObject.getValue("result")
        val valueType = nameTypeMap[id]
        val value = when (valueType) {
          "bool" -> jsonValue.jsonPrimitive.boolean
          "real" -> jsonValue.jsonPrimitive.double
          "usint" -> jsonValue.jsonPrimitive.double
          "dint" -> jsonValue.jsonPrimitive.double
          "uint" -> jsonValue.jsonPrimitive.double
          "int" -> jsonValue.jsonPrimitive.double
          "string" -> jsonValue.jsonPrimitive.content
          else -> throw IllegalArgumentException("unknown datatype: $valueType ($jsonValue)")
        }

        id to value
      }
    } else {
      return emptyList()
    }
  }
}