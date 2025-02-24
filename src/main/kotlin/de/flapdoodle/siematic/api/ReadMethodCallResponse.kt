package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

object ReadMethodCallResponse {
  @Serializable
  data class ReadNumber(
    var jsonrpc: String,
    var id: String,
    var result: Double
  )

  @Serializable
  data class ReadBoolean(
    var jsonrpc: String,
    var id: String,
    var result: Boolean
  )
}
