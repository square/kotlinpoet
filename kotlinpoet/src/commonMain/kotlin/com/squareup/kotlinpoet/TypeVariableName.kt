/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("TypeVariableNames")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.jvm.alias.JvmType
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance

public class TypeVariableName private constructor(
  public val name: String,
  public val bounds: List<TypeName>,

  /** Either [KModifier.IN], [KModifier.OUT], or null. */
  public val variance: KModifier? = null,
  public val isReified: Boolean = false,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap(),
) : TypeName(nullable, annotations, TagMap(tags)) {

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>,
  ): TypeVariableName {
    return copy(nullable, annotations, this.bounds, this.isReified, tags)
  }

  public fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
    bounds: List<TypeName> = this.bounds.toList(),
    reified: Boolean = this.isReified,
    tags: Map<KClass<*>, Any> = this.tagMap.tags,
  ): TypeVariableName {
    return TypeVariableName(
      name,
      bounds.withoutImplicitBound(),
      variance,
      reified,
      nullable,
      annotations,
      tags,
    )
  }

  private fun List<TypeName>.withoutImplicitBound(): List<TypeName> {
    return if (size == 1) this else filterNot { it == NULLABLE_ANY }
  }

  override fun emit(out: CodeWriter) = out.emit(name)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TypeVariableName) return false
    if (!super.equals(other)) return false

    if (name != other.name) return false
    if (bounds != other.bounds) return false
    if (variance != other.variance) return false
    if (isReified != other.isReified) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + bounds.hashCode()
    result = 31 * result + (variance?.hashCode() ?: 0)
    result = 31 * result + isReified.hashCode()
    return result
  }

  public companion object {
    internal fun of(
      name: String,
      bounds: List<TypeName>,
      variance: KModifier?,
    ): TypeVariableName {
      require(variance == null || variance.isOneOf(KModifier.IN, KModifier.OUT)) {
        "$variance is an invalid variance modifier, the only allowed values are in and out!"
      }
      require(bounds.isNotEmpty()) {
        "$name has no bounds"
      }
      // Strip Any? from bounds if it is present.
      return TypeVariableName(name, bounds, variance)
    }

    /** Returns type variable named `name` with `variance` and without bounds. */
    @JvmStatic
    @JvmName("get")
    @JvmOverloads
    public operator fun invoke(name: String, variance: KModifier? = null): TypeVariableName =
      of(name = name, bounds = NULLABLE_ANY_LIST, variance = variance)

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("get")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      vararg bounds: TypeName,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name = name,
        bounds = bounds.toList().ifEmpty(::NULLABLE_ANY_LIST),
        variance = variance,
      )

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("get")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      vararg bounds: KClass<*>,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name = name,
        bounds = bounds.map(KClass<*>::asTypeName).ifEmpty(::NULLABLE_ANY_LIST),
        variance = variance,
      )

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("get")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      vararg bounds: JvmType,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name = name,
        bounds = bounds.map(JvmType::asTypeName).ifEmpty(::NULLABLE_ANY_LIST),
        variance = variance,
      )

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("get")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      bounds: List<TypeName>,
      variance: KModifier? = null,
    ): TypeVariableName = of(name, bounds.ifEmpty(::NULLABLE_ANY_LIST), variance)

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("getWithClasses")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      bounds: Iterable<KClass<*>>,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name,
        bounds.map { it.asTypeName() }.ifEmpty(::NULLABLE_ANY_LIST),
        variance,
      )

    /** Returns type variable named `name` with `variance` and `bounds`. */
    @JvmStatic
    @JvmName("getWithTypes")
    @JvmOverloads
    public operator fun invoke(
      name: String,
      bounds: Iterable<JvmType>,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name,
        bounds.map { it.asTypeName() }.ifEmpty(::NULLABLE_ANY_LIST),
        variance,
      )

    internal val NULLABLE_ANY_LIST = listOf(NULLABLE_ANY)
  }
}

public fun KTypeParameter.asTypeVariableName(): TypeVariableName {
  return TypeVariableName.of(
    name = name,
    bounds = upperBounds.map(KType::asTypeName)
      .ifEmpty(TypeVariableName.Companion::NULLABLE_ANY_LIST),
    variance = when (variance) {
      KVariance.INVARIANT -> null
      KVariance.IN -> KModifier.IN
      KVariance.OUT -> KModifier.OUT
    },
  )
}
