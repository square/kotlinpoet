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
import java.util.concurrent.atomic.AtomicReference

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
    assertThrows<IllegalArgumentException> {
      TypeAliasSpec.builder("null", String::class)
    }
  }

  @Test fun typeVariable() {
    val v = TypeVariableName("V")
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", ParameterizedTypeName.get(List::class.asClassName(), v))
        .addTypeVariable(v)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |typealias Word<V> = kotlin.collections.List<V>
        |""".trimMargin())
  }

  @Test fun publicVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .addModifiers(KModifier.PUBLIC)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |public typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun internalVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .addModifiers(KModifier.INTERNAL)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |internal typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun privateVisibility() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .addModifiers(KModifier.PRIVATE)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |private typealias Word = kotlin.String
        |""".trimMargin())
  }

  @Test fun implTypeAlias() {
    val typeName = ParameterizedTypeName.get(
        AtomicReference::class.asClassName(),
        TypeVariableName("V"))
    val typeAliasSpec = TypeAliasSpec
        .builder("AtomicRef<V>", typeName)
        .addModifiers(KModifier.ACTUAL)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
        |actual typealias AtomicRef<V> = java.util.concurrent.atomic.AtomicReference<V>
        |""".trimMargin())
  }

  @Test fun kdoc() {
    val typeAliasSpec = TypeAliasSpec
        .builder("Word", String::class)
        .addKdoc("Word is just a type alias for [String](%T).\n", String::class)
        .build()

    assertThat(typeAliasSpec.toString()).isEqualTo("""
      |/**
      | * Word is just a type alias for [String](kotlin.String).
      | */
      |typealias Word = kotlin.String
      |""".trimMargin())
  }

  @Test fun equalsAndHashCode() {
    val a = TypeAliasSpec.builder("Word", String::class).addModifiers(KModifier.PUBLIC).build()
    val b = TypeAliasSpec.builder("Word", String::class).addModifiers(KModifier.PUBLIC).build()

    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }
}
