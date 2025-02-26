package de.flapdoodle.siematic

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PropertyCollectorTest {

  @Test
  fun addTree() {
    val testee = PropertyCollector()

    testee.add("foo.bar.baz")
    testee.value("ping.pong",2)

    val result = testee.render()

    assertThat(result)
      .isEqualTo("\n" +
          "foo\n" +
          "  bar\n" +
          "    baz\n" +
          "ping\n" +
          "  pong = 2\n" +
          "")
  }
}