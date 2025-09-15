/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.util.concurrent.atomic.AtomicReference
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.TYPEALIAS
import kotlin.test.Test

class TypeAliasSpecTest {

  @Test fun simpleTypeAlias() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |public typealias Word = kotlin.String
        |
      """.trimMargin(),
    )
  }

  @Test fun typeVariable() {
    val v = TypeVariableName("V")
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", List::class.asClassName().parameterizedBy(v))
      .addTypeVariable(v)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |public typealias Word<V> = kotlin.collections.List<V>
        |
      """.trimMargin(),
    )
  }

  @Test fun publicVisibility() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addModifiers(KModifier.PUBLIC)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |public typealias Word = kotlin.String
        |
      """.trimMargin(),
    )
  }

  @Test fun internalVisibility() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addModifiers(KModifier.INTERNAL)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |internal typealias Word = kotlin.String
        |
      """.trimMargin(),
    )
  }

  @Test fun privateVisibility() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addModifiers(KModifier.PRIVATE)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |private typealias Word = kotlin.String
        |
      """.trimMargin(),
    )
  }

  @Test fun implTypeAlias() {
    val typeName = AtomicReference::class.asClassName().parameterizedBy(TypeVariableName("V"))
    val typeAliasSpec = TypeAliasSpec
      .builder("AtomicRef", typeName)
      .addModifiers(KModifier.ACTUAL)
      .addTypeVariable(TypeVariableName("V"))
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
        |public actual typealias AtomicRef<V> = java.util.concurrent.atomic.AtomicReference<V>
        |
      """.trimMargin(),
    )
  }

  @Test fun kdoc() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addKdoc("Word is just a type alias for [String](%T).\n", String::class)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
      |/**
      | * Word is just a type alias for [String](kotlin.String).
      | */
      |public typealias Word = kotlin.String
      |
      """.trimMargin(),
    )
  }

  @Test fun annotations() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addAnnotation(
        AnnotationSpec.builder(TypeAliasAnnotation::class.asClassName())
          .addMember("value = %S", "words!")
          .build(),
      )
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
      |@com.squareup.kotlinpoet.TypeAliasAnnotation(value = "words!")
      |public typealias Word = kotlin.String
      |
      """.trimMargin(),
    )
  }

  @Test fun kdocWithoutNewLine() {
    val typeAliasSpec = TypeAliasSpec
      .builder("Word", String::class)
      .addKdoc("Word is just a type alias for [String](%T).", String::class)
      .build()

    assertThat(typeAliasSpec.toString()).isEqualTo(
      """
      |/**
      | * Word is just a type alias for [String](kotlin.String).
      | */
      |public typealias Word = kotlin.String
      |
      """.trimMargin(),
    )
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

    assertThat(builder.build().modifiers).containsExactlyInAnyOrder(KModifier.INTERNAL)
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

  @Test fun modifyAnnotations() {
    val builder = TypeAliasSpec
      .builder("Word", String::class)
      .addAnnotation(
        AnnotationSpec.builder(TypeAliasAnnotation::class.asClassName())
          .addMember("value = %S", "value1")
          .build(),
      )

    val javaWord = AnnotationSpec.builder(TypeAliasAnnotation::class.asClassName())
      .addMember("value = %S", "value2")
      .build()
    builder.annotations.clear()
    builder.annotations.add(javaWord)

    assertThat(builder.build().annotations).containsExactly(javaWord)
  }

  @Test fun nameEscaping() {
    val typeAlias = TypeAliasSpec.builder("fun", String::class).build()
    assertThat(typeAlias.toString()).isEqualTo(
      """
      |public typealias `fun` = kotlin.String
      |
      """.trimMargin(),
    )
  }

  @Test fun annotatedLambdaType() {
    val annotation = AnnotationSpec.builder(ClassName("", "Annotation")).build()
    val type = LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation))
    val spec = TypeAliasSpec.builder("lambda", type).build()
    assertThat(spec.toString()).isEqualTo(
      """
      |public typealias lambda = @Annotation () -> kotlin.Unit
      |
      """.trimMargin(),
    )
  }
}

@Retention(RUNTIME)
@Target(TYPEALIAS)
annotation class TypeAliasAnnotation(val value: String)
