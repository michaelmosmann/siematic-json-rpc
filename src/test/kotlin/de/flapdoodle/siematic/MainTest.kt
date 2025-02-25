package de.flapdoodle.siematic

import de.flapdoodle.siematic.api.ArrayDimensions
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MainTest {

  @Test
  fun simpleOffset() {
    val result = ArrayDimensions.unroll(listOf(ArrayDimensions(1,2)))

    assertThat(result)
      .containsExactly("1","2")
  }

  @Test
  fun multiArray() {
    val result = ArrayDimensions.unroll(listOf(ArrayDimensions(1,2),ArrayDimensions(1,2)))

    assertThat(result)
      .containsExactly("1,1","1,2","2,1","2,2")
  }
}