package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class ArrayDimensions(
  var start_index: Int,
  var count: Int
)