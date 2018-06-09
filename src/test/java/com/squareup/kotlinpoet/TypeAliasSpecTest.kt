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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

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
        .builder("Word", List::class.asClassName().parameterizedBy(v))
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
    val typeName = AtomicReference::class.asClassName().parameterizedBy(TypeVariableName("V"))
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

  @Test fun generalBuilderEqualityTest() {
    val typeParam = TypeVariableName("V")
    val typeAliasSpec = TypeAliasSpec
        .builder("Bio", Pair::class.parameterizedBy(String::class, String::class))
        .addKdoc("First nand Last Name.\n")
        .addModifiers(KModifier.PUBLIC)
        .addTypeVariable(typeParam)
        .build()
    assertThat(typeAliasSpec.toBuilder().build()).isEqualTo(typeAliasSpec)
  }

  @Test fun modifyModifiers() {
    val builder = TypeAliasSpec
        .builder("Word", String::class)
        .addModifiers(KModifier.PRIVATE)

    builder.modifiers.clear()
    builder.modifiers.add(KModifier.INTERNAL)

    assertThat(builder.build().modifiers).containsExactly(KModifier.INTERNAL)
  }

  @Test fun modifyTypeVariableNames() {
    val builder = TypeAliasSpec
        .builder("Word", String::class)
        .addTypeVariable(TypeVariableName("V"))

    val tVar = TypeVariableName("T")
    builder.typeVariables.clear()
    builder.typeVariables.add(tVar)

    assertThat(builder.build().typeVariables).containsExactly(tVar)
  }
}
