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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.util.Map

class AnnotatedTypeNameTest {
  private val NEVER_NULL = AnnotationSpec.builder(NeverNull::class.java).build()
  private val NN = NeverNull::class.java.canonicalName

  annotation class NeverNull

  @Test fun annotated() {
    val simpleString = TypeName.get(String::class.java)
    assertFalse(simpleString.isAnnotated)
    assertEquals(simpleString, TypeName.get(String::class.java))
    val annotated = simpleString.annotated(NEVER_NULL)
    assertTrue(annotated.isAnnotated)
    assertEquals(annotated, annotated.annotated())
  }

  @Test fun annotatedType() {
    val expected = "@$NN java.lang.String"
    val type = TypeName.get(String::class.java)
    val actual = type.annotated(NEVER_NULL).toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedTwice() {
    val expected = "@$NN @java.lang.Override java.lang.String"
    val type = TypeName.get(String::class.java)
    val actual = type.annotated(NEVER_NULL)
        .annotated(AnnotationSpec.builder(Override::class.java).build())
        .toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedParameterizedType() {
    val expected = "@$NN java.util.List<java.lang.String>"
    val type = ParameterizedTypeName.get(List::class.java, String::class.java)
    val actual = type.annotated(NEVER_NULL).toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedArgumentOfParameterizedType() {
    val expected = "java.util.List<@$NN java.lang.String>"
    val type = TypeName.get(String::class.java).annotated(NEVER_NULL)
    val list = ClassName.get(List::class.java)
    val actual = ParameterizedTypeName.get(list, type).toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedWildcardTypeNameWithSuper() {
    val expected = "in @$NN java.lang.String"
    val type = TypeName.get(String::class.java).annotated(NEVER_NULL)
    val actual = WildcardTypeName.supertypeOf(type).toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedWildcardTypeNameWithExtends() {
    val expected = "out @$NN java.lang.String"
    val type = TypeName.get(String::class.java).annotated(NEVER_NULL)
    val actual = WildcardTypeName.subtypeOf(type).toString()
    assertEquals(expected, actual)
  }

  @Test fun annotatedEquivalence() {
    annotatedEquivalence(UNIT)
    annotatedEquivalence(ArrayTypeName.of(Object::class.java))
    annotatedEquivalence(ClassName.get(Any::class.java))
    annotatedEquivalence(ParameterizedTypeName.get(List::class.java, Any::class.java))
    annotatedEquivalence(TypeVariableName.get("A"))
    annotatedEquivalence(WildcardTypeName.subtypeOf(Object::class))
  }

  private fun annotatedEquivalence(type: TypeName) {
    assertFalse(type.isAnnotated)
    assertEquals(type, type)
    assertEquals(type.annotated(NEVER_NULL), type.annotated(NEVER_NULL))
    assertNotEquals(type, type.annotated(NEVER_NULL))
    assertEquals(type.hashCode().toLong(), type.hashCode().toLong())
    assertEquals(type.annotated(NEVER_NULL).hashCode().toLong(), type.annotated(NEVER_NULL).hashCode().toLong())
    assertNotEquals(type.hashCode().toLong(), type.annotated(NEVER_NULL).hashCode().toLong())
  }

  // https://github.com/square/javapoet/issues/431
  // @Target(ElementType.TYPE_USE) requires Java 1.8
  annotation class TypeUseAnnotation

  // https://github.com/square/javapoet/issues/431
  @Ignore @Test fun annotatedNestedType() {
    val expected = "java.util.Map.@" + TypeUseAnnotation::class.java.canonicalName + " Entry"
    val typeUseAnnotation = AnnotationSpec.builder(TypeUseAnnotation::class.java).build()
    val type = TypeName.get(Map.Entry::class.java).annotated(typeUseAnnotation)
    val actual = type.toString()
    assertEquals(expected, actual)
  }

  // https://github.com/square/javapoet/issues/431
  @Ignore @Test fun annotatedNestedParameterizedType() {
    val expected = "java.util.Map.@" + TypeUseAnnotation::class.java.canonicalName +
        " Entry<java.lang.Byte, java.lang.Byte>"
    val typeUseAnnotation = AnnotationSpec.builder(TypeUseAnnotation::class.java).build()
    val type = ParameterizedTypeName.get(Map.Entry::class.java, Byte::class.java, Byte::class.java)
        .annotated(typeUseAnnotation)
    val actual = type.toString()
    assertEquals(expected, actual)
  }
}
