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
package com.squareup.kotlinpoet.javapoet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.javapoet.ArrayTypeName
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.ENUM
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.typeNameOf
import org.junit.Test

@OptIn(KotlinPoetJavaPoetPreview::class)
class PoetInteropTest {

  @Test
  fun classNamesMatch() {
    val kotlinPoetCN = PoetInteropTest::class.asClassName()
    val javapoetCN = kotlinPoetCN.toJClassName()

    assertThat(javapoetCN.toKTypeName()).isEqualTo(kotlinPoetCN)
    assertThat(JClassName.get(PoetInteropTest::class.java)).isEqualTo(javapoetCN)
  }

  @Test
  fun nestedClassNamesMatch() {
    val kotlinPoetCN = PoetInteropTest::class.asClassName().nestedClass("Foo").nestedClass("Bar")
    val javapoetCN = kotlinPoetCN.toJClassName()

    assertThat(javapoetCN.toKTypeName()).isEqualTo(kotlinPoetCN)
    assertThat(JClassName.get(PoetInteropTest::class.java).nestedClass("Foo").nestedClass("Bar"))
      .isEqualTo(javapoetCN)
  }

  @Test
  fun kotlinIntrinsicsMapCorrectlyToJava() {
    // To Java
    assertThat(LIST.toJTypeName()).isEqualTo(PoetInterop.CN_JAVA_LIST)
    assertThat(SET.toJTypeName()).isEqualTo(PoetInterop.CN_JAVA_SET)
    assertThat(MAP.toJTypeName()).isEqualTo(PoetInterop.CN_JAVA_MAP)
    assertThat(STRING.toJTypeName()).isEqualTo(PoetInterop.CN_JAVA_STRING)
    assertThat(ANY.toJTypeName()).isEqualTo(JTypeName.OBJECT)

    // To Kotlin
    assertThat(PoetInterop.CN_JAVA_LIST.toKTypeName()).isEqualTo(LIST)
    assertThat(PoetInterop.CN_JAVA_SET.toKTypeName()).isEqualTo(SET)
    assertThat(PoetInterop.CN_JAVA_MAP.toKTypeName()).isEqualTo(MAP)
    assertThat(PoetInterop.CN_JAVA_STRING.toKTypeName()).isEqualTo(STRING)
    assertThat(JTypeName.OBJECT.toKTypeName()).isEqualTo(ANY)
  }

  @Test
  fun boxIfPrimitiveRequestReturnsBoxedPrimitive() {
    assertThat(BOOLEAN.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.BOOLEAN.box())
    assertThat(BYTE.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.BYTE.box())
    assertThat(CHAR.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.CHAR.box())
    assertThat(SHORT.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.SHORT.box())
    assertThat(INT.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.INT.box())
    assertThat(LONG.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.LONG.box())
    assertThat(FLOAT.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.FLOAT.box())
    assertThat(DOUBLE.toJTypeName(boxIfPrimitive = true)).isEqualTo(JTypeName.DOUBLE.box())
  }

  @Test
  fun primitivesAreUnboxedByDefault() {
    assertThat(BOOLEAN.toJTypeName()).isEqualTo(JTypeName.BOOLEAN)
    assertThat(BYTE.toJTypeName()).isEqualTo(JTypeName.BYTE)
    assertThat(CHAR.toJTypeName()).isEqualTo(JTypeName.CHAR)
    assertThat(SHORT.toJTypeName()).isEqualTo(JTypeName.SHORT)
    assertThat(INT.toJTypeName()).isEqualTo(JTypeName.INT)
    assertThat(LONG.toJTypeName()).isEqualTo(JTypeName.LONG)
    assertThat(FLOAT.toJTypeName()).isEqualTo(JTypeName.FLOAT)
    assertThat(DOUBLE.toJTypeName()).isEqualTo(JTypeName.DOUBLE)
  }

  @Test
  fun nullablePrimitiveBoxedByDefault() {
    assertThat(BOOLEAN.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.BOOLEAN.box())
    assertThat(BYTE.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.BYTE.box())
    assertThat(CHAR.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.CHAR.box())
    assertThat(SHORT.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.SHORT.box())
    assertThat(INT.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.INT.box())
    assertThat(LONG.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.LONG.box())
    assertThat(FLOAT.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.FLOAT.box())
    assertThat(DOUBLE.copy(nullable = true).toJTypeName()).isEqualTo(JTypeName.DOUBLE.box())
  }

  @Test
  fun arrayTypesConversion() {
    assertThat(ARRAY.parameterizedBy(INT).toJParameterizedOrArrayTypeName())
      .isEqualTo(ArrayTypeName.of(JTypeName.INT))
    assertThat(ARRAY.parameterizedBy(INT.copy(nullable = true)).toJParameterizedOrArrayTypeName())
      .isEqualTo(ArrayTypeName.of(JTypeName.INT.box()))
    assertThat(ArrayTypeName.of(JTypeName.INT).toKTypeName()).isEqualTo(INT_ARRAY)
    assertThat(ArrayTypeName.of(JTypeName.INT.box()).toKTypeName())
      .isEqualTo(ARRAY.parameterizedBy(INT))
  }

  class GenericType<T>

  @Test
  fun wildcards() {
    val inKType = typeNameOf<GenericType<in String>>()
    val superJType = JParameterizedTypeName.get(
      JClassName.get(GenericType::class.java),
      JWildcardTypeName.supertypeOf(String::class.java),
    )
    assertThat(inKType.toJTypeName()).isEqualTo(superJType)
    assertThat(superJType.toKTypeName().toString()).isEqualTo(inKType.toString())

    val outKType = typeNameOf<GenericType<out String>>()
    val extendsJType = JParameterizedTypeName.get(
      JClassName.get(GenericType::class.java),
      JWildcardTypeName.subtypeOf(String::class.java),
    )
    assertThat(outKType.toJTypeName()).isEqualTo(extendsJType)
    assertThat(extendsJType.toKTypeName().toString()).isEqualTo(outKType.toString())

    val star = typeNameOf<GenericType<*>>()
    val extendsObjectJType = JParameterizedTypeName.get(
      JClassName.get(GenericType::class.java),
      JWildcardTypeName.subtypeOf(JTypeName.OBJECT),
    )
    assertThat(star.toJTypeName()).isEqualTo(extendsObjectJType)
    assertThat(extendsObjectJType.toKTypeName().toString()).isEqualTo(star.toString())
    assertThat(STAR.toJTypeName()).isEqualTo(JWildcardTypeName.subtypeOf(JTypeName.OBJECT))
    assertThat(JWildcardTypeName.subtypeOf(JTypeName.OBJECT).toKTypeName()).isEqualTo(STAR)
  }

  @Test
  fun complex() {
    val complexType = typeNameOf<Map<String?, List<MutableMap<Int, IntArray>>>>()
    val jType = JParameterizedTypeName.get(
      JClassName.get(Map::class.java),
      JClassName.get(String::class.java),
      JParameterizedTypeName.get(
        JClassName.get(List::class.java),
        JParameterizedTypeName.get(
          JClassName.get(Map::class.java),
          JClassName.INT.box(),
          ArrayTypeName.of(JClassName.INT),
        ),
      ),
    )
    assertThat(complexType.toJTypeName()).isEqualTo(jType)

    assertThat(jType.toKTypeName())
      .isEqualTo(typeNameOf<Map<String, List<MutableMap<Int, IntArray>>>>())
  }

  @Test
  fun uTypesAreJustNormalTypesInJava() {
    assertThat(U_BYTE.toJTypeName()).isEqualTo(JTypeName.BYTE)
    assertThat(U_SHORT.toJTypeName()).isEqualTo(JTypeName.SHORT)
    assertThat(U_INT.toJTypeName()).isEqualTo(JTypeName.INT)
    assertThat(U_LONG.toJTypeName()).isEqualTo(JTypeName.LONG)
  }

  @Test
  fun enums() {
    assertThat(ENUM.toJTypeName()).isEqualTo(PoetInterop.CN_JAVA_ENUM)
    assertThat(PoetInterop.CN_JAVA_ENUM.toKTypeName()).isEqualTo(ENUM)
  }
}
