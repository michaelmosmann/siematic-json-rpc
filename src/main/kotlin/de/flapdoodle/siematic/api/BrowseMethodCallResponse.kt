package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class BrowseMethodCallResponse(
  var jsonrpc: String,
  var id: String,
  var result: List<PropertyResponse>
)