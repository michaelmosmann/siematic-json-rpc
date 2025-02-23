package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class PropertyResponse(
  var name: String,
  var read_only: Boolean,
  var has_children: Boolean,
  var db_number: Int,
  var datatype: String,
  var array_dimensions: List<ArrayDimensions>? = null,
)