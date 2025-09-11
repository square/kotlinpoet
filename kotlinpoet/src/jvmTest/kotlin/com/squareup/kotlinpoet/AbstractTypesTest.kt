/*
 * Copyright (C) 2014 Google, Inc.
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
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.Serializable
import java.nio.charset.Charset
import javax.lang.model.element.Element
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.fail

abstract class AbstractTypesTest {
  protected abstract val elements: Elements
  protected abstract val types: Types

  private fun getElement(`class`: Class<*>) = elements.getTypeElement(`class`.canonicalName)

  private fun getMirror(`class`: Class<*>) = getElement(`class`).asType()

  @Test fun getBasicTypeMirror() {
    assertThat(getMirror(Any::class.java).asTypeName())
      .isEqualTo(Any::class.java.asClassName())
    assertThat(getMirror(Charset::class.java).asTypeName())
      .isEqualTo(Charset::class.asClassName())
    assertThat(getMirror(AbstractTypesTest::class.java).asTypeName())
      .isEqualTo(AbstractTypesTest::class.asClassName())
  }

  @Test fun getParameterizedTypeMirror() {
    val setType = types.getDeclaredType(getElement(Set::class.java), getMirror(String::class.java))
    assertThat(setType.asTypeName())
      .isEqualTo(Set::class.asClassName().parameterizedBy(String::class.asClassName()))
  }

  @Test fun getErrorType() {
    val errorType = DeclaredTypeAsErrorType(types.getDeclaredType(getElement(Set::class.java)))
    assertThat(errorType.asTypeName()).isEqualTo(Set::class.asClassName())
  }

  internal class Parameterized<
    Simple,
    ExtendsClass : Number,
    ExtendsInterface : Runnable,
    ExtendsTypeVariable : Simple,
    Intersection,
    IntersectionOfInterfaces>
  where IntersectionOfInterfaces : Runnable, Intersection : Number, Intersection : Runnable,
        IntersectionOfInterfaces : Serializable

  @Test fun getTypeVariableTypeMirror() {
    val typeVariables = getElement(Parameterized::class.java).typeParameters

    // Members of converted types use ClassName and not Class<?>.
    val number = Number::class.asClassName()
    val runnable = Runnable::class.asClassName()
    val serializable = Serializable::class.asClassName()

    assertThat(typeVariables[0].asType().asTypeName())
      .isEqualTo(TypeVariableName("Simple"))
    assertThat(typeVariables[1].asType().asTypeName())
      .isEqualTo(TypeVariableName("ExtendsClass", number))
    assertThat(typeVariables[2].asType().asTypeName())
      .isEqualTo(TypeVariableName("ExtendsInterface", runnable))
    assertThat(typeVariables[3].asType().asTypeName())
      .isEqualTo(TypeVariableName("ExtendsTypeVariable", TypeVariableName("Simple")))
    assertThat(typeVariables[4].asType().asTypeName())
      .isEqualTo(TypeVariableName("Intersection", number, runnable))
    assertThat(typeVariables[5].asType().asTypeName())
      .isEqualTo(TypeVariableName("IntersectionOfInterfaces", runnable, serializable))
    assertThat((typeVariables[4].asType().asTypeName() as TypeVariableName).bounds)
      .containsExactlyInAnyOrder(number, runnable)
  }

  internal class Recursive<T : Map<List<T>, Set<Array<T>>>>

  @Test fun getTypeVariableTypeMirrorRecursive() {
    val typeMirror = getElement(Recursive::class.java).asType()
    val typeName = typeMirror.asTypeName() as ParameterizedTypeName
    val className = Recursive::class.java.canonicalName
    assertThat(typeName.toString()).isEqualTo("$className<T>")

    val typeVariableName = typeName.typeArguments[0] as TypeVariableName
    assertThat(typeVariableName.toString()).isEqualTo("T")
    assertThat(typeVariableName.bounds.toString())
      .isEqualTo("[kotlin.collections.Map<kotlin.collections.List<out T>, out kotlin.collections.Set<out kotlin.Array<T>>>]")
  }

  @Test fun getPrimitiveTypeMirror() {
    assertThat(types.getPrimitiveType(TypeKind.BOOLEAN).asTypeName()).isEqualTo(BOOLEAN)
    assertThat(types.getPrimitiveType(TypeKind.BYTE).asTypeName()).isEqualTo(BYTE)
    assertThat(types.getPrimitiveType(TypeKind.SHORT).asTypeName()).isEqualTo(SHORT)
    assertThat(types.getPrimitiveType(TypeKind.INT).asTypeName()).isEqualTo(INT)
    assertThat(types.getPrimitiveType(TypeKind.LONG).asTypeName()).isEqualTo(LONG)
    assertThat(types.getPrimitiveType(TypeKind.CHAR).asTypeName()).isEqualTo(CHAR)
    assertThat(types.getPrimitiveType(TypeKind.FLOAT).asTypeName()).isEqualTo(FLOAT)
    assertThat(types.getPrimitiveType(TypeKind.DOUBLE).asTypeName()).isEqualTo(DOUBLE)
  }

  @Test fun getArrayTypeMirror() {
    assertThat(types.getArrayType(getMirror(String::class.java)).asTypeName())
      .isEqualTo(ARRAY.parameterizedBy(String::class.asClassName()))
  }

  @Test fun getVoidTypeMirror() {
    assertThat(types.getNoType(TypeKind.VOID).asTypeName()).isEqualTo(UNIT)
  }

  @Test fun getNullTypeMirror() {
    try {
      types.nullType.asTypeName()
      fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test fun parameterizedType() {
    val type = Map::class.parameterizedBy(String::class, Long::class)
    assertThat(type.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Long>")
  }

  @Test fun starProjection() {
    assertThat(STAR.toString()).isEqualTo("*")
  }

  @Ignore("Figure out what this maps to in Kotlin.")
  @Test fun starProjectionFromMirror() {
    val wildcard = types.getWildcardType(null, null)
    val type = wildcard.asTypeName()
    assertThat(type.toString()).isEqualTo("*")
  }

  @Test fun varianceOutType() {
    val type = WildcardTypeName.producerOf(CharSequence::class)
    assertThat(type.toString()).isEqualTo("out java.lang.CharSequence")
  }

  @Test fun varianceOutTypeFromMirror() {
    val types = types
    val elements = elements
    val charSequence = elements.getTypeElement(CharSequence::class.java.name).asType()
    val wildcard = types.getWildcardType(charSequence, null)
    val type = wildcard.asTypeName()
    assertThat(type.toString()).isEqualTo("out java.lang.CharSequence")
  }

  @Test fun varianceInType() {
    val type = WildcardTypeName.consumerOf(String::class)
    assertThat(type.toString()).isEqualTo("in kotlin.String")
  }

  @Test fun varianceInTypeFromMirror() {
    val types = types
    val elements = elements
    val string = elements.getTypeElement(String::class.java.name).asType()
    val wildcard = types.getWildcardType(null, string)
    val type = wildcard.asTypeName()
    assertThat(type.toString()).isEqualTo("in kotlin.String")
  }

  @Test fun typeVariable() {
    val type = TypeVariableName("T", CharSequence::class)
    assertThat(type.toString()).isEqualTo("T") // (Bounds are only emitted in declaration.)
  }

  @Test fun kType() {
    assertThat(Map::class.starProjectedType.asTypeName().toString())
      .isEqualTo("kotlin.collections.Map<*, *>")
    assertThat(Map::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, String::class.createType(emptyList())), KTypeProjection.STAR)).asTypeName().toString())
      .isEqualTo("kotlin.collections.Map<kotlin.String, *>")
    assertThat(Map.Entry::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, String::class.createType(emptyList())), KTypeProjection.STAR)).asTypeName().toString())
      .isEqualTo("kotlin.collections.Map.Entry<kotlin.String, *>")
    assertThat(Any::class.starProjectedType.withNullability(true).asTypeName().toString())
      .isEqualTo("kotlin.Any?")

    val treeMapClass = java.util.TreeMap::class
    assertThat(treeMapClass.declaredFunctions.find { it.name == "parentOf" }!!.returnType.asTypeName().toString())
      .isEqualTo("java.util.TreeMap.Entry<K, V>")
  }

  private class DeclaredTypeAsErrorType(private val declaredType: DeclaredType) : ErrorType {
    override fun asElement(): Element = declaredType.asElement()

    override fun getEnclosingType(): TypeMirror = declaredType.enclosingType

    override fun getTypeArguments(): MutableList<out TypeMirror> = declaredType.typeArguments

    override fun getKind(): TypeKind = declaredType.kind

    override fun <R, P> accept(typeVisitor: TypeVisitor<R, P>, p: P): R = typeVisitor.visitError(this, p)

    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A> =
      throw UnsupportedOperationException()

    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A = throw UnsupportedOperationException()

    override fun getAnnotationMirrors() = throw UnsupportedOperationException()
  }
}
