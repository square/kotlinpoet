/*
 * Copyright (C) 2017 Square, Inc.
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
import kotlin.test.fail

class TypeAliasSpecTest {

  @Test fun simpleTypeAlias() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .build()
    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun keywordForbiddenAsTypeAliasName() {
    try {
      TypeAliasSpec
          .builder("null", String::class)
      fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test fun publicVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .visibility(KModifier.PUBLIC)
        .build()
    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |public typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun internalVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .visibility(KModifier.INTERNAL)
        .build()
    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |internal typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun privateVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .visibility(KModifier.PRIVATE)
        .build()
    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |private typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun equalsAndHashCode() {
    val a = TypeAliasSpec.builder("Word", String::class).visibility(KModifier.PUBLIC).build()
    val b = TypeAliasSpec.builder("Word", String::class).visibility(KModifier.PUBLIC).build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
