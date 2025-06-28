/*
 * Copyright (C) 2018 Square, Inc.
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

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.kotlinFunction
import org.junit.Test

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
    val invariantInt = KTypeProjection(KVariance.INVARIANT, Int::class.createType())
    val typeName = Array<Unit>::class.createType(listOf(invariantInt)).asTypeName()
    assertThat(typeName.toString()).isEqualTo("kotlin.Array<kotlin.Int>")
  }

  @Test fun arrayPlusObjectParameter() {
    val invariantCloseable = KTypeProjection(KVariance.INVARIANT, Closeable::class.createType())
    val typeName = Array<Unit>::class.createType(listOf(invariantCloseable)).asTypeName()
    assertThat(typeName.toString()).isEqualTo("kotlin.Array<java.io.Closeable>")
  }

  @Test fun arrayPlusNullableParameter() {
    val invariantNullableCloseable = KTypeProjection(KVariance.INVARIANT, Closeable::class.createType(nullable = true))
    val typeName = Array<Unit>::class.createType(listOf(invariantNullableCloseable)).asTypeName()
    assertThat(typeName.toString()).isEqualTo("kotlin.Array<java.io.Closeable?>")
  }

  @Test fun typeParameter() {
    val funWithParam: () -> Closeable = this::withParam
    val typeName = (funWithParam as KFunction<*>).returnType.asTypeName()
    assertThat(typeName.toString()).isEqualTo("Param")
  }

  @Test fun nullableTypeParameter() {
    val funWithParam: () -> Closeable? = this::withNullableParam
    val typeName = (funWithParam as KFunction<*>).returnType.asTypeName()
    assertThat(typeName.toString()).isEqualTo("Param?")
  }

  @Test fun classPlusTwoParameters() {
    val typeName = java.util.Map::class.java
      .plusParameter(java.lang.String::class.java)
      .plusParameter(java.lang.Integer::class.java)
    assertThat(typeName.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>")
  }

  @Test fun copyingTypeArguments() {
    val typeName = java.util.Map::class.java
      .plusParameter(java.lang.String::class.java)
      .plusParameter(java.lang.Integer::class.java)
      .nestedClass(
        "Entry",
        listOf(
          java.lang.String::class.java.asClassName(),
          java.lang.Integer::class.java.asClassName(),
        ),
      )
      .copy(typeArguments = listOf(STAR, STAR))
    assertThat(typeName.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>.Entry<*, *>")
  }

  interface Projections {
    val outVariance: KClass<out Annotation>
    val inVariance: KClass<in Test>
    val invariantNullable: KClass<Test>?
    val star: KClass<*>
    val multiVariant: Map<in String, List<Map<KClass<out Number>, *>?>>
    val outAnyOnTypeWithoutBoundsAndVariance: KMutableProperty<out Any>
  }

  private fun assertKTypeProjections(kType: KType) = assertThat(kType.asTypeName().toString()).isEqualTo(kType.toString())

  @Test fun kTypeOutProjection() = assertKTypeProjections(Projections::outVariance.returnType)

  @Test fun kTypeInProjection() = assertKTypeProjections(Projections::inVariance.returnType)

  @Test fun kTypeInvariantNullableProjection() = assertKTypeProjections(Projections::invariantNullable.returnType)

  @Test fun kTypeStarProjection() = assertKTypeProjections(Projections::star.returnType)

  @Test fun kTypeMultiVariantProjection() = assertKTypeProjections(Projections::multiVariant.returnType)

  @Test fun kTypeOutAnyOnTypeWithoutBoundsVariance() = assertKTypeProjections(Projections::outAnyOnTypeWithoutBoundsAndVariance.returnType)

  private fun <Param : Closeable> withParam(): Param = throw NotImplementedError("for testing purposes")

  private fun <Param : Closeable> withNullableParam(): Param? = throw NotImplementedError("for testing purposes")

  @Test fun annotatedLambdaTypeParameter() {
    val annotation = AnnotationSpec.builder(ClassName("", "Annotation")).build()
    val typeName = Map::class.asTypeName()
      .plusParameter(String::class.asTypeName())
      .plusParameter(LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation)))
    assertThat(typeName.toString())
      .isEqualTo("kotlin.collections.Map<kotlin.String, @Annotation () -> kotlin.Unit>")
  }

  private class Enclosing1 {
    class GenericClass<T>
  }

  private object Enclosing2 {
    class Foo
  }

  @Test fun equalsAndHashCode() {
    val parameterizedTypeName1 = Enclosing1.GenericClass::class.parameterizedBy(Enclosing2.Foo::class)
    val parameterizedTypeName2 = Enclosing1.GenericClass::class.parameterizedBy(Enclosing2.Foo::class)
    assertThat(parameterizedTypeName1).isEqualTo(parameterizedTypeName2)
    assertThat(parameterizedTypeName1.hashCode()).isEqualTo(parameterizedTypeName2.hashCode())

    assertThat(parameterizedTypeName1.copy(nullable = true)).isNotEqualTo(parameterizedTypeName1)

    assertThat(parameterizedTypeName1.copy(annotations = listOf(AnnotationSpec.builder(Suppress::class).build()))).isNotEqualTo(parameterizedTypeName1)
  }

  @Test fun equalsAndHashCodeIgnoreTags() {
    val parameterizedTypeName = Enclosing1.GenericClass::class.parameterizedBy(Enclosing2.Foo::class)
    val tagged = parameterizedTypeName.copy(tags = mapOf(String::class to "test"))

    assertThat(parameterizedTypeName).isEqualTo(tagged)
    assertThat(parameterizedTypeName.hashCode()).isEqualTo(tagged.hashCode())
  }

  // https://github.com/square/kotlinpoet/issues/1914
  @Test fun stackOverflowOnRecursivelyBoundGeneric() {
    val method = Class.forName("kotlin.collections.ArraysKt").methods.first {
      it.name == "max" && it.parameters.first().parameterizedType.typeName.contains("Comparable")
    }
    val kotlinFunction = method.kotlinFunction
    assertThat(kotlinFunction).isNotNull()
    val parameter = kotlinFunction!!.parameters.first()
    val typeName = parameter.type.asTypeName()
    // Should produce a valid TypeName without stack overflow
    assertThat(typeName.toString()).contains("T")
  }
}
