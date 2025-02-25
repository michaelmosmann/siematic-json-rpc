package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
data class ArrayDimensions(
  var start_index: Int,
  var count: Int
) {
  companion object {
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
  }
}