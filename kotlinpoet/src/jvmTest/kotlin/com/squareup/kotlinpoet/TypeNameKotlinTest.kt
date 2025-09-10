/*
 * Copyright (C) 2021 Square, Inc.
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
import assertk.assertions.isEqualTo
import org.junit.Test

class TypeNameKotlinTest {

  @Test
  fun typeNameOf_simple() {
    val type = typeNameOf<TypeNameKotlinTest>()
    assertThat(type.toString()).isEqualTo("com.squareup.kotlinpoet.TypeNameKotlinTest")
  }

  @Test
  fun typeNameOf_simple_intrinsic() {
    val type = typeNameOf<String>()
    assertThat(type.toString()).isEqualTo("kotlin.String")
  }

  @Test
  fun typeNameOf_array_primitive() {
    val type = typeNameOf<IntArray>()
    assertThat(type.toString()).isEqualTo("kotlin.IntArray")
  }

  @Test
  fun typeNameOf_array_parameterized() {
    val type = typeNameOf<Array<String>>()
    assertThat(type.toString()).isEqualTo("kotlin.Array<kotlin.String>")
  }

  @Test
  fun typeNameOf_nullable() {
    val type = typeNameOf<String?>()
    assertThat(type.toString()).isEqualTo("kotlin.String?")
  }

  @Test
  fun typeNameOf_generic() {
    val type = typeNameOf<List<String>>()
    assertThat(type.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
  }

  @Test
  fun typeNameOf_generic_wildcard_out() {
    val type = typeNameOf<GenericType<out String>>()
    assertThat(type.toString()).isEqualTo("com.squareup.kotlinpoet.TypeNameKotlinTest.GenericType<out kotlin.String>")
  }

  @Test
  fun typeNameOf_generic_wildcard_in() {
    val type = typeNameOf<GenericType<in String>>()
    assertThat(type.toString()).isEqualTo("com.squareup.kotlinpoet.TypeNameKotlinTest.GenericType<in kotlin.String>")
  }

  @Test
  fun typeNameOf_complex() {
    val type = typeNameOf<Map<String, List<Map<*, GenericType<in Set<Array<GenericType<out String>?>>>>>>>()
    assertThat(type.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.collections.Map<*, com.squareup.kotlinpoet.TypeNameKotlinTest.GenericType<in kotlin.collections.Set<kotlin.Array<com.squareup.kotlinpoet.TypeNameKotlinTest.GenericType<out kotlin.String>?>>>>>>")
  }

  @Suppress("unused")
  class GenericType<T>

  @Test
  fun tag() {
    val type = typeNameOf<String>().copy(tags = mapOf(String::class to "Test"))
    assertThat(type.tag<String>()).isEqualTo("Test")
  }

  @Test
  fun existingTagsShouldBePreserved() {
    val type = typeNameOf<String>().copy(tags = mapOf(String::class to "Test"))
    val copied = type.copy(nullable = true)
    assertThat(copied.tag<String>()).isEqualTo("Test")
  }
}
