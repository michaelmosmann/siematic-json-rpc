package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class Error(
  var code: Int,
  var message: String,
)