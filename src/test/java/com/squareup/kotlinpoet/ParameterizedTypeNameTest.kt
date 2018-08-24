/*
 * Copyright (C) 2018 Square, Inc.
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import org.junit.Test
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

class ParameterizedTypeNameTest {
  @Test fun classNamePlusParameter() {
    val typeName = ClassName("kotlin.collections", "List")
        .plusParameter(ClassName("kotlin", "String"))
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
  }

  @Test fun classNamePlusTwoParameters() {
    val typeName = ClassName("kotlin.collections", "Map")
        .plusParameter(ClassName("kotlin", "String"))
        .plusParameter(ClassName("kotlin", "Int"))
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Int>")
  }

  @Test fun classNamePlusTypeVariableParameter() {
    val t = TypeVariableName("T")
    val mapOfT = Map::class.asTypeName().plusParameter(t)
    assertThat(mapOfT.toString()).isEqualTo("kotlin.collections.Map<T>")
  }

  @Test fun kClassPlusParameter() {
    val typeName = List::class.plusParameter(String::class)
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
  }

  @Test fun kClassPlusTwoParameters() {
    val typeName = Map::class
        .plusParameter(String::class)
        .plusParameter(Int::class)
    assertThat(typeName.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Int>")
  }

  @Test fun classPlusParameter() {
    val typeName = java.util.List::class.java.plusParameter(java.lang.String::class.java)
    assertThat(typeName.toString()).isEqualTo("java.util.List<java.lang.String>")
  }

  @Test fun primitiveArray() {
    assertThat(ByteArray::class.asTypeName().toString()).isEqualTo("kotlin.ByteArray")
    assertThat(CharArray::class.asTypeName().toString()).isEqualTo("kotlin.CharArray")
    assertThat(ShortArray::class.asTypeName().toString()).isEqualTo("kotlin.ShortArray")
    assertThat(IntArray::class.asTypeName().toString()).isEqualTo("kotlin.IntArray")
    assertThat(LongArray::class.asTypeName().toString()).isEqualTo("kotlin.LongArray")
    assertThat(FloatArray::class.asTypeName().toString()).isEqualTo("kotlin.FloatArray")
    assertThat(DoubleArray::class.asTypeName().toString()).isEqualTo("kotlin.DoubleArray")
  }

  @Test fun arrayPlusPrimitiveParameter() {
    val typeName = Array<Int>::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, Int::class.createType()))).asTypeName()
    assertThat(typeName.toString()).isEqualTo("kotlin.Array<kotlin.Int>")
  }

  @Test fun arrayPlusObjectParameter() {
    val typeName = Array<Unit>::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, Closeable::class.createType()))).asTypeName()
    assertThat(typeName.toString()).isEqualTo("kotlin.Array<java.io.Closeable>")
  }

  @Test fun classPlusTwoParameters() {
    val typeName = java.util.Map::class.java
        .plusParameter(java.lang.String::class.java)
        .plusParameter(java.lang.Integer::class.java)
    assertThat(typeName.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>")
  }

  interface Projections {
    val outVariance: KClass<out Annotation>
    val inVariance: KClass<in Test>
    val invariantNullable: KClass<Test>?
    val star: KClass<*>
    val multiVariant: Map<in String, List<Map<KClass<out Number>, *>?>>
  }

  private fun assertKTypeProjections(kType: KType) = assertThat(kType.asTypeName().toString()).isEqualTo(kType.toString())

  @Test fun kTypeOutProjection() = assertKTypeProjections(Projections::outVariance.returnType)

  @Test fun kTypeInProjection() = assertKTypeProjections(Projections::inVariance.returnType)

  @Test fun kTypeInvariantNullableProjection() = assertKTypeProjections(Projections::invariantNullable.returnType)

  @Test fun kTypeStarProjection() = assertKTypeProjections(Projections::star.returnType)

  @Test fun kTypeMultiVariantProjection() = assertKTypeProjections(Projections::multiVariant.returnType)
}
