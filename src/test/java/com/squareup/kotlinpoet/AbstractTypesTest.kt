/*
 * Copyright (C) 2014 Google, Inc.
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
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import java.io.Serializable
import java.nio.charset.Charset
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVisitor
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

abstract class AbstractTypesTest {
  protected abstract val elements: Elements
  protected abstract val types: Types

  private fun getElement(clazz: Class<*>) = elements.getTypeElement(clazz.canonicalName)

  private fun getMirror(clazz: Class<*>) = getElement(clazz).asType()

  @Test fun getBasicTypeMirror() {
    assertThat(TypeName.get(getMirror(Any::class.java)))
        .isEqualTo(ClassName.get(Any::class.java))
    assertThat(TypeName.get(getMirror(Charset::class.java)))
        .isEqualTo(ClassName.get(Charset::class))
    assertThat(TypeName.get(getMirror(AbstractTypesTest::class.java)))
        .isEqualTo(ClassName.get(AbstractTypesTest::class))
  }

  @Test fun getParameterizedTypeMirror() {
    val setType = types.getDeclaredType(getElement(Set::class.java), getMirror(String::class.java))
    assertThat(TypeName.get(setType))
        .isEqualTo(
            ParameterizedTypeName.get(ClassName.get(Set::class), ClassName.get(String::class)))
  }

  @Test fun getErrorType() {
    val errorType = DeclaredTypeAsErrorType(types.getDeclaredType(getElement(Set::class.java)))
    assertThat(TypeName.get(errorType)).isEqualTo(ClassName.get(Set::class))
  }

  internal class Parameterized<
      Simple,
      ExtendsClass : Number,
      ExtendsInterface : Runnable,
      ExtendsTypeVariable : Simple,
      Intersection : Number,
      IntersectionOfInterfaces : Runnable>
  where Intersection : Runnable, IntersectionOfInterfaces : Serializable

  @Test fun getTypeVariableTypeMirror() {
    val typeVariables = getElement(Parameterized::class.java).typeParameters

    // Members of converted types use ClassName and not Class<?>.
    val number = ClassName.get(Number::class)
    val runnable = ClassName.get(Runnable::class)
    val serializable = ClassName.get(Serializable::class)

    assertThat(TypeName.get(typeVariables[0].asType()))
        .isEqualTo(TypeVariableName.get("Simple"))
    assertThat(TypeName.get(typeVariables[1].asType()))
        .isEqualTo(TypeVariableName.get("ExtendsClass", number))
    assertThat(TypeName.get(typeVariables[2].asType()))
        .isEqualTo(TypeVariableName.get("ExtendsInterface", runnable))
    assertThat(TypeName.get(typeVariables[3].asType()))
        .isEqualTo(TypeVariableName.get("ExtendsTypeVariable", TypeVariableName.get("Simple")))
    assertThat(TypeName.get(typeVariables[4].asType()))
        .isEqualTo(TypeVariableName.get("Intersection", number, runnable))
    assertThat(TypeName.get(typeVariables[5].asType()))
        .isEqualTo(TypeVariableName.get("IntersectionOfInterfaces", runnable, serializable))
    assertThat((TypeName.get(typeVariables[4].asType()) as TypeVariableName).bounds)
        .containsExactly(number, runnable)
  }

  internal class Recursive<T : Map<List<T>, Set<Array<T>>>>

  @Test fun getTypeVariableTypeMirrorRecursive() {
    val typeMirror = getElement(Recursive::class.java).asType()
    val typeName = TypeName.get(typeMirror) as ParameterizedTypeName
    val className = Recursive::class.java.canonicalName
    assertThat(typeName.toString()).isEqualTo(className + "<T>")

    val typeVariableName = typeName.typeArguments[0] as TypeVariableName
    assertThat(typeVariableName.toString()).isEqualTo("T")
    assertThat(typeVariableName.bounds.toString())
        .isEqualTo("[kotlin.collections.Map<kotlin.collections.List<out T>, out kotlin.collections.Set<out kotlin.Array<T>>>]")
  }

  @Test fun getPrimitiveTypeMirror() {
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.BOOLEAN))).isEqualTo(BOOLEAN)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.BYTE))).isEqualTo(BYTE)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.SHORT))).isEqualTo(SHORT)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.INT))).isEqualTo(INT)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.LONG))).isEqualTo(LONG)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.CHAR))).isEqualTo(CHAR)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.FLOAT))).isEqualTo(FLOAT)
    assertThat(TypeName.get(types.getPrimitiveType(TypeKind.DOUBLE))).isEqualTo(DOUBLE)
  }

  @Test fun getArrayTypeMirror() {
    assertThat(TypeName.get(types.getArrayType(getMirror(String::class.java))))
        .isEqualTo(ParameterizedTypeName.get(ARRAY, ClassName.get(String::class)))
  }

  @Test fun getVoidTypeMirror() {
    assertThat(TypeName.get(types.getNoType(TypeKind.VOID))).isEqualTo(UNIT)
  }

  @Test fun getNullTypeMirror() {
    try {
      TypeName.get(types.nullType)
      fail()
    } catch (expected: IllegalArgumentException) {
    }
  }

  @Test fun parameterizedType() {
    val type = ParameterizedTypeName.get(Map::class, String::class, Long::class)
    assertThat(type.toString()).isEqualTo("kotlin.collections.Map<kotlin.String, kotlin.Long>")
  }

  @Test fun starProjection() {
    val type = WildcardTypeName.subtypeOf(ANY)
    assertThat(type.toString()).isEqualTo("*")
  }

  @Ignore("Figure out what this maps to in Kotlin.")
  @Test fun starProjectionFromMirror() {
    val wildcard = types.getWildcardType(null, null)
    val type = TypeName.get(wildcard)
    assertThat(type.toString()).isEqualTo("*")
  }

  @Test fun varianceOutType() {
    val type = WildcardTypeName.subtypeOf(CharSequence::class)
    assertThat(type.toString()).isEqualTo("out java.lang.CharSequence")
  }

  @Test fun varianceOutTypeFromMirror() {
    val types = types
    val elements = elements
    val charSequence = elements.getTypeElement(CharSequence::class.java.name).asType()
    val wildcard = types.getWildcardType(charSequence, null)
    val type = TypeName.get(wildcard)
    assertThat(type.toString()).isEqualTo("out java.lang.CharSequence")
  }

  @Test fun varianceInType() {
    val type = WildcardTypeName.supertypeOf(String::class)
    assertThat(type.toString()).isEqualTo("in kotlin.String")
  }

  @Test fun varianceInTypeFromMirror() {
    val types = types
    val elements = elements
    val string = elements.getTypeElement(String::class.java.name).asType()
    val wildcard = types.getWildcardType(null, string)
    val type = TypeName.get(wildcard)
    assertThat(type.toString()).isEqualTo("in kotlin.String")
  }

  @Test fun typeVariable() {
    val type = TypeVariableName.get("T", CharSequence::class)
    assertThat(type.toString()).isEqualTo("T") // (Bounds are only emitted in declaration.)
  }

  private class DeclaredTypeAsErrorType(private val declaredType: DeclaredType) : ErrorType {
    override fun asElement() = declaredType.asElement()

    override fun getEnclosingType() = declaredType.enclosingType

    override fun getTypeArguments() = declaredType.typeArguments

    override fun getKind() = declaredType.kind

    override fun <R, P> accept(typeVisitor: TypeVisitor<R, P>, p: P)
        = typeVisitor.visitError(this, p)

    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A>
        = throw UnsupportedOperationException()

    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A
        = throw UnsupportedOperationException()

    override fun getAnnotationMirrors()
        = throw UnsupportedOperationException()
  }
}
