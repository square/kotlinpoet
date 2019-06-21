/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class NameAllocatorTest {
  @Test fun usage() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("foo", 1)).isEqualTo("foo")
    assertThat(nameAllocator.newName("bar", 2)).isEqualTo("bar")
    assertThat(nameAllocator[1]).isEqualTo("foo")
    assertThat(nameAllocator[2]).isEqualTo("bar")
  }

  @Test fun nameCollision() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo")
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo_")
    assertThat(nameAllocator.newName("foo")).isEqualTo("foo__")
  }

  @Test fun nameCollisionWithTag() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("foo", 1)).isEqualTo("foo")
    assertThat(nameAllocator.newName("foo", 2)).isEqualTo("foo_")
    assertThat(nameAllocator.newName("foo", 3)).isEqualTo("foo__")
    assertThat(nameAllocator[1]).isEqualTo("foo")
    assertThat(nameAllocator[2]).isEqualTo("foo_")
    assertThat(nameAllocator[3]).isEqualTo("foo__")
  }

  @Test fun characterMappingSubstitute() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("a-b", 1)).isEqualTo("a_b")
  }

  @Test fun characterMappingSurrogate() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("a\uD83C\uDF7Ab", 1)).isEqualTo("a_b")
  }

  @Test fun characterMappingInvalidStartButValidPart() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("1ab", 1)).isEqualTo("_1ab")
  }

  @Test fun characterMappingInvalidStartIsInvalidPart() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("&ab", 1)).isEqualTo("_ab")
  }

  @Test fun kotlinKeyword() {
    val nameAllocator = NameAllocator()
    assertThat(nameAllocator.newName("when", 1)).isEqualTo("when_")
    assertThat(nameAllocator[1]).isEqualTo("when_")
  }

  @Test fun tagReuseForbidden() {
    val nameAllocator = NameAllocator()
    nameAllocator.newName("foo", 1)
    assertThrows<IllegalArgumentException> {
      nameAllocator.newName("bar", 1)
    }.hasMessageThat().isEqualTo("tag 1 cannot be used for both 'foo' and 'bar'")
  }

  @Test fun useBeforeAllocateForbidden() {
    val nameAllocator = NameAllocator()
    assertThrows<IllegalArgumentException> {
      nameAllocator[1]
    }.hasMessageThat().isEqualTo("unknown tag: 1")
  }

  @Test fun cloneUsage() {
    val outerAllocator = NameAllocator()
    outerAllocator.newName("foo", 1)

    val innerAllocator1 = outerAllocator.copy()
    assertThat(innerAllocator1.newName("bar", 2)).isEqualTo("bar")
    assertThat(innerAllocator1.newName("foo", 3)).isEqualTo("foo_")

    val innerAllocator2 = outerAllocator.copy()
    assertThat(innerAllocator2.newName("foo", 2)).isEqualTo("foo_")
    assertThat(innerAllocator2.newName("bar", 3)).isEqualTo("bar")
  }
}
