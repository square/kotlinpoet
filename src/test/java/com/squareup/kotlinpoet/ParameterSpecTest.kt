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

class ParameterSpecTest {
  @Test fun equalsAndHashCode() {
    var a = ParameterSpec.builder("foo", Int::class)
        .build()
    var b = ParameterSpec.builder("foo", Int::class)
        .build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = ParameterSpec.builder("i", Int::class)
        .addModifiers(KModifier.FINAL)
        .build()
    b = ParameterSpec.builder("i", Int::class)
        .addModifiers(KModifier.FINAL)
        .build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun escapeKeywordInParameterName() {
    val parameterSpec = ParameterSpec.builder("if", String::class)
        .build()
    assertThat(parameterSpec.toString()).isEqualTo("`if`: kotlin.String")
  }
}
