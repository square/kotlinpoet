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

import kotlin.reflect.KClass

class WildcardTypeName internal constructor(
    upperBounds: List<TypeName>,
    lowerBounds: List<TypeName>,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()) : TypeName(nullable, annotations) {

  val upperBounds = upperBounds.toImmutableList()
  val lowerBounds = lowerBounds.toImmutableList()

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

  override fun emit(out: CodeWriter): CodeWriter {
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

    @JvmStatic fun subtypeOf(upperBound: KClass<*>): WildcardTypeName {
      return subtypeOf(upperBound.asTypeName())
    }

    /**
     * Returns a type that represents an unknown supertype of `bound`. For example, if `bound` is
     * `String.class`, this returns `? super String`.
     */
    @JvmStatic fun supertypeOf(lowerBound: TypeName): WildcardTypeName {
      return WildcardTypeName(listOf(ANY), listOf(lowerBound))
    }

    @JvmStatic fun supertypeOf(lowerBound: KClass<*>): WildcardTypeName {
      return supertypeOf(lowerBound.asTypeName())
    }
  }
}
