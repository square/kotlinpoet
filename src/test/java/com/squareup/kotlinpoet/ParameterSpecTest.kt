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

  @Test fun escapePunctuationInParameterName() {
    val parameterSpec = ParameterSpec.builder("with-hyphen", String::class)
        .build()
    assertThat(parameterSpec.toString()).isEqualTo("`with-hyphen`: kotlin.String")
  }

  @Test fun generalBuilderEqualityTest() {
    val parameterSpec = ParameterSpec.builder("Nuts", String::class)
        .addAnnotation(ClassName("com.squareup.kotlinpoet", "Food"))
        .addModifiers(KModifier.VARARG)
        .defaultValue("Almonds")
        .build()

    assertThat(parameterSpec.toBuilder().build()).isEqualTo(parameterSpec)
  }

  @Test fun modifyModifiers() {
    val builder = ParameterSpec
        .builder("word", String::class)
        .addModifiers(KModifier.PRIVATE)

    builder.modifiers.clear()
    builder.modifiers.add(KModifier.INTERNAL)

    assertThat(builder.build().modifiers).containsExactly(KModifier.INTERNAL)
  }

  @Test fun modifyAnnotations() {
    val builder = ParameterSpec
        .builder("word", String::class)
        .addAnnotation(AnnotationSpec.builder(JvmName::class.asClassName())
            .addMember("name = %S", "jvmWord")
            .build())

    val javaWord = AnnotationSpec.builder(JvmName::class.asClassName())
        .addMember("name = %S", "javaWord")
        .build()
    builder.annotations.clear()
    builder.annotations.add(javaWord)

    assertThat(builder.build().annotations).containsExactly(javaWord)
  }
}
