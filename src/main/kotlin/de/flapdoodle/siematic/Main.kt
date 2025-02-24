package de.flapdoodle.siematic

import de.flapdoodle.net.Net
import de.flapdoodle.siematic.api.ApiLogin
import de.flapdoodle.siematic.api.ApiLoginResponse
import de.flapdoodle.siematic.api.BrowseMethodCallResponse
import de.flapdoodle.siematic.api.MethodCall
import de.flapdoodle.siematic.api.ReadMethodCallResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

  inline fun <reified T> response(request: HttpRequest): T {
    val response = HttpClient.newBuilder()
//    .proxy(ProxySelector.getDefault())
      .sslContext(Net.acceptAllSSLContext())
      .build()
      .send(request, BodyHandlers.ofString())

    require(response.statusCode() == 200) {
      "request failed: $request, status: ${response.statusCode()}"
    }

    val body = response.body()
    try {
      return Json.decodeFromString<T>(body)
    } catch (e: Exception) {
      throw RuntimeException("failed to parse response:\n${response.body()}\n", e)
    }
  }

  fun readValue(token: String, name: String, datatype: String): Any {
    val request = request(MethodCall.read("id", name),"x-auth-token" to token)
    return when (datatype) {
      "real" -> response<ReadMethodCallResponse.ReadNumber>(request).result
      "bool" -> response<ReadMethodCallResponse.ReadBoolean>(request).result
      
      else -> throw IllegalArgumentException("unknown datatype: $datatype")
    }
  }

  fun browse(token: String, name: String, level: Int = 0) {
    println("${"  ".repeat(level)}${name}")

    val browseRequest = request(MethodCall.browse("id", name),"x-auth-token" to token)
    val browseResult = response<BrowseMethodCallResponse>(browseRequest)
    browseResult.result.forEach {
      //println("${"  ".repeat(level + 1)}${it.name} (${it.datatype})")
//      println("---")
//      println("-> $it")
      if (it.has_children) {
        when (it.datatype) {
          "struct" -> {
            val arrayDimensions = it.array_dimensions
            if (arrayDimensions!=null) {
              require(arrayDimensions.size == 1) { "more or less than one array dimension" }
              val arrayDimension = arrayDimensions[0]
              (arrayDimension.start_index..<(arrayDimension.start_index+arrayDimension.count)).forEach { offset ->
                browse(token, "$name.${it.name}[$offset]", level + 1)
              }
            } else {
              browse(token, "$name.${it.name}", level + 1)
            }
          }
        }
      } else {
        val value = readValue(token, "$name.${it.name}", it.datatype)
        println("${"  ".repeat(level + 1)}$name.${it.name} = $value (${it.datatype})")
      }
//      println("---")
    }
  }

  @JvmStatic
  fun main(vararg args: String) {
    val request = request(ApiLogin.loginWith("Service", "2010"))
    val tokenResponse = response<ApiLoginResponse>(request)
    val token = tokenResponse.result.token
    println("token: ${token}")

    browse(token, "\"dbMode\"")
  }
}

