package de.flapdoodle.siematic.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModeVar(
  var mode: String = "",
  @SerialName("var")
  var varName: String = ""
)