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

import java.io.IOException
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import javax.lang.model.element.TypeParameterElement
import kotlin.reflect.KClass

class WildcardTypeName private constructor(
    upperBounds: List<TypeName>,
    lowerBounds: List<TypeName>,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()) : TypeName(nullable, annotations) {

  val upperBounds: List<TypeName> = upperBounds.toImmutableList()
  val lowerBounds: List<TypeName> = lowerBounds.toImmutableList()

  init {
    require(this.upperBounds.size == 1) { "unexpected extends bounds: $upperBounds" }
  }

  override fun asNullable() = WildcardTypeName(upperBounds, lowerBounds, true, annotations)

  override fun asNonNullable() = WildcardTypeName(upperBounds, lowerBounds, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>): WildcardTypeName {
    return WildcardTypeName(upperBounds, lowerBounds, nullable, this.annotations + annotations)
  }

  override fun withoutAnnotations(): TypeName {
    return WildcardTypeName(upperBounds, lowerBounds, nullable)
  }

  @Throws(IOException::class)
  override fun abstractEmit(out: CodeWriter): CodeWriter {
    if (lowerBounds.size == 1) {
      return out.emitCode("in %T", lowerBounds[0])
    }
    return if (upperBounds[0] == ANY)
      out.emit("*") else
      out.emitCode("out %T", upperBounds[0])
  }

  companion object {
    /**
     * Returns a type that represents an unknown type that extends `bound`. For example, if `bound`
     * is `CharSequence.class`, this returns `? extends CharSequence`. If `bound` is `Object.class`,
     * this returns `?`, which is shorthand for `? extends Object`.
     */
    @JvmStatic fun subtypeOf(upperBound: TypeName): WildcardTypeName {
      return WildcardTypeName(listOf(upperBound), emptyList())
    }

    @JvmStatic fun subtypeOf(upperBound: Type): WildcardTypeName {
      return subtypeOf(TypeName.get(upperBound))
    }

    @JvmStatic fun subtypeOf(upperBound: KClass<*>): WildcardTypeName {
      return subtypeOf(TypeName.get(upperBound))
    }

    /**
     * Returns a type that represents an unknown supertype of `bound`. For example, if `bound` is
     * `String.class`, this returns `? super String`.
     */
    @JvmStatic fun supertypeOf(lowerBound: TypeName): WildcardTypeName {
      return WildcardTypeName(listOf(ANY), listOf(lowerBound))
    }

    @JvmStatic fun supertypeOf(lowerBound: Type): WildcardTypeName {
      return supertypeOf(TypeName.get(lowerBound))
    }

    @JvmStatic fun supertypeOf(lowerBound: KClass<*>): WildcardTypeName {
      return supertypeOf(TypeName.get(lowerBound))
    }

    @JvmOverloads @JvmStatic fun get(
        mirror: javax.lang.model.type.WildcardType,
        typeVariables: MutableMap<TypeParameterElement, TypeVariableName> = mutableMapOf())
        : TypeName {
      val extendsBound = mirror.extendsBound
      if (extendsBound == null) {
        val superBound = mirror.superBound
        return if (superBound == null)
          subtypeOf(ANY) else
          supertypeOf(TypeName.get(superBound, typeVariables))
      } else {
        return subtypeOf(TypeName.get(extendsBound, typeVariables))
      }
    }

    @JvmStatic fun get(
        wildcardName: WildcardType,
        map: MutableMap<Type, TypeVariableName> = mutableMapOf())
        : TypeName {
      return WildcardTypeName(
          wildcardName.upperBounds.map { TypeName.get(it, map = map) },
          wildcardName.lowerBounds.map { TypeName.get(it, map = map) })
    }
  }
}
