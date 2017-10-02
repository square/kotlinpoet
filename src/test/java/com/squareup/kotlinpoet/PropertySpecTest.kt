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
import org.junit.Test

class PropertySpecTest {
  @Test fun nullable() {
    val type = String::class.asClassName().asNullable()
    val a = PropertySpec.builder("foo", type).build()
    assertThat(a.toString()).isEqualTo("val foo: kotlin.String?\n")
  }

  @Test fun delegated() {
    val prop = PropertySpec.builder("foo", String::class)
        .delegate("Delegates.notNull()")
        .build()
    assertThat(prop.toString()).isEqualTo("val foo: kotlin.String by Delegates.notNull()\n")
  }

  @Test fun inlineSingleAccessor() {
    val prop = PropertySpec.builder("foo", String::class)
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return %S", "foo")
            .build())
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |val foo: kotlin.String
      |  inline get() = "foo"
      |""".trimMargin())
  }

  @Test fun inlineBothAccessors() {
    val prop = PropertySpec.varBuilder("foo", String::class)
        .getter(FunSpec.getterBuilder()
            .addModifiers(KModifier.INLINE)
            .addStatement("return %S", "foo")
            .build())
        .setter(FunSpec.setterBuilder()
            .addModifiers(KModifier.INLINE)
            .addParameter("value", String::class)
            .build())
        .build()

    assertThat(prop.toString()).isEqualTo("""
      |inline var foo: kotlin.String
      |  get() = "foo"
      |  set(value) {
      |  }
      |""".trimMargin())
  }

  @Test fun inlineForbiddenOnProperty() {
    assertThrows<IllegalArgumentException> {
      PropertySpec.builder("foo", String::class)
          .addModifiers(KModifier.INLINE)
    }
  }

  @Test fun equalsAndHashCode() {
    val type = Int::class
    var a = PropertySpec.builder("foo", type).build()
    var b = PropertySpec.builder("foo", type).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = PropertySpec.builder("FOO", type, KModifier.PUBLIC, KModifier.LATEINIT).build()
    b = PropertySpec.builder("FOO", type, KModifier.PUBLIC, KModifier.LATEINIT).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
