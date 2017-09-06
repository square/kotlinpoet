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
@file:JvmName("TypeVariableNames")

package com.squareup.kotlinpoet

import kotlin.reflect.KClass

class TypeVariableName internal constructor(
    val name: String,
    val bounds: List<TypeName>,
    val variance: KModifier? = null,
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList())
  : TypeName(nullable, annotations) {

  override fun asNullable() = TypeVariableName(name, bounds, variance, true, annotations)

  override fun asNonNullable() = TypeVariableName(name, bounds, variance, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>): TypeVariableName {
    return TypeVariableName(name, bounds, variance, nullable, annotations)
  }

  override fun withoutAnnotations(): TypeName {
    return TypeVariableName(name, bounds, variance, nullable)
  }

  fun withBounds(vararg bounds: KClass<*>): TypeVariableName {
    return withBounds(bounds.map { it.asTypeName() })
  }

  fun withBounds(vararg bounds: TypeName): TypeVariableName {
    return withBounds(bounds.toList())
  }

  fun withBounds(bounds: List<TypeName>): TypeVariableName {
    return TypeVariableName(name, this.bounds + bounds, variance, nullable, annotations)
  }

  override fun emit(out: CodeWriter): CodeWriter {
    return out.emit(name)
  }

  companion object {
    internal fun of(name: String, bounds: List<TypeName>, variance: KModifier?): TypeVariableName {
      require(variance == null || variance == KModifier.IN || variance == KModifier.OUT) {
        "$variance is an invalid variance modifier, the only allowed values are in and out!"
      }
      // Strip java.lang.Object from bounds if it is present.
      return TypeVariableName(name, bounds.filter { it != ANY }, variance)
    }

    /** Returns type variable named `name` with `variance` and without bounds.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, variance: KModifier? = null) =
        TypeVariableName.of(name, emptyList(), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: TypeName, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.toList(), variance)

    /** Returns type variable named `name` with `variance` and `bounds`.  */
    @JvmStatic @JvmName("get") @JvmOverloads
    operator fun invoke(name: String, vararg bounds: KClass<*>, variance: KModifier? = null) =
        TypeVariableName.of(name, bounds.map { it.asTypeName() }, variance)
  }
}
