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
import javax.lang.model.element.Modifier

class PropertySpecTest {
  @Test fun equalsAndHashCode() {
    val type = Int::class.javaPrimitiveType!!
    var a = PropertySpec.builder(type, "foo").build()
    var b = PropertySpec.builder(type, "foo").build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = PropertySpec.builder(type, "FOO", Modifier.PUBLIC, Modifier.STATIC)
        .build()
    b = PropertySpec.builder(type, "FOO", Modifier.PUBLIC, Modifier.STATIC)
        .build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
