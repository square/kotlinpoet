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
@file:JvmName("WildcardTypeNames")

package com.squareup.kotlinpoet

import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import javax.lang.model.element.TypeParameterElement
import kotlin.reflect.KClass

class WildcardTypeName private constructor(
  upperBounds: List<TypeName>,
  lowerBounds: List<TypeName>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  val upperBounds = upperBounds.toImmutableList()
  val lowerBounds = lowerBounds.toImmutableList()

  init {
    require(this.upperBounds.size == 1) { "unexpected extends bounds: $upperBounds" }
  }

  override fun asNullable() = WildcardTypeName(upperBounds, lowerBounds, true, annotations)

  override fun asNonNull() = WildcardTypeName(upperBounds, lowerBounds, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>): WildcardTypeName {
    return WildcardTypeName(upperBounds, lowerBounds, nullable, this.annotations + annotations)
  }

  override fun withoutAnnotations() = WildcardTypeName(upperBounds, lowerBounds, nullable)

  override fun emit(out: CodeWriter): CodeWriter {
    return when {
      lowerBounds.size == 1 -> out.emitCode("in %T", lowerBounds[0])
      upperBounds == STAR.upperBounds -> out.emit("*")
      else -> out.emitCode("out %T", upperBounds[0])
    }
  }

  companion object {
    /**
     * Returns a type that represents an unknown type that extends `upperBound`. For example, if
     * `upperBound` is `CharSequence`, this returns `out CharSequence`. If `upperBound` is `Any?`,
     * this returns `*`, which is shorthand for `out Any?`.
     */
    @JvmStatic fun subtypeOf(upperBound: TypeName) =
      WildcardTypeName(listOf(upperBound), emptyList())

    @JvmStatic fun subtypeOf(upperBound: Type) = subtypeOf(upperBound.asTypeName())

    @JvmStatic fun subtypeOf(upperBound: KClass<*>) = subtypeOf(upperBound.asTypeName())

    /**
     * Returns a type that represents an unknown supertype of `lowerBound`. For example, if
     * `lowerBound` is `String`, this returns `in String`.
     */
    @JvmStatic fun supertypeOf(lowerBound: TypeName) =
      WildcardTypeName(listOf(ANY), listOf(lowerBound))

    @JvmStatic fun supertypeOf(lowerBound: Type) = supertypeOf(lowerBound.asTypeName())

    @JvmStatic fun supertypeOf(lowerBound: KClass<*>) = supertypeOf(lowerBound.asTypeName())

    internal fun get(
      mirror: javax.lang.model.type.WildcardType,
      typeVariables: Map<TypeParameterElement, TypeVariableName>
    ): TypeName {
      val extendsBound = mirror.extendsBound
      if (extendsBound == null) {
        val superBound = mirror.superBound
        return if (superBound == null) {
          STAR
        } else {
          supertypeOf(TypeName.get(superBound, typeVariables))
        }
      } else {
        return subtypeOf(TypeName.get(extendsBound, typeVariables))
      }
    }

    internal fun get(
      wildcardName: WildcardType,
      map: MutableMap<Type, TypeVariableName>
    ): TypeName {
      return WildcardTypeName(
          wildcardName.upperBounds.map { TypeName.get(it, map = map) },
          wildcardName.lowerBounds.map { TypeName.get(it, map = map) })
    }
  }
}

@JvmName("get")
fun javax.lang.model.type.WildcardType.asWildcardTypeName()
    = WildcardTypeName.get(this, mutableMapOf())

@JvmName("get")
fun WildcardType.asWildcardTypeName() = WildcardTypeName.get(this, mutableMapOf())
