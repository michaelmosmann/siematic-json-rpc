package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class PropertyResponse(
  var name: String,
  var address: String? = null,
  var area: String? = null,
  var max_length: Int? = null,
  var read_only: Boolean = false,
  var has_children: Boolean = false,
  var db_number: Int? = null,
  var datatype: String,
  var array_dimensions: List<ArrayDimensions>? = null,
) {
  fun childNames(prefix: String): List<String> {
    val isRoot = prefix.isEmpty()
    val arrayDimensions = array_dimensions
    val escapedName = "\"$name\""
    return if (arrayDimensions==null) {
      listOf(if (isRoot) escapedName else "$prefix.$escapedName")
    } else {
      val all = ArrayDimensions.unroll(arrayDimensions)
      all.map { if (isRoot) "$escapedName[$it]" else "$prefix.$escapedName[$it]" }
    }
  }
}