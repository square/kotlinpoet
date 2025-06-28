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

package com.squareup.kotlinpoet

import java.lang.reflect.Type
import java.util.Collections
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import kotlin.reflect.KClass
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
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as TypeVariableName

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
      vararg bounds: Type,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name = name,
        bounds = bounds.map(Type::asTypeName).ifEmpty(::NULLABLE_ANY_LIST),
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
      bounds: Iterable<Type>,
      variance: KModifier? = null,
    ): TypeVariableName =
      of(
        name,
        bounds.map { it.asTypeName() }.ifEmpty(::NULLABLE_ANY_LIST),
        variance,
      )

    /**
     * Make a TypeVariableName for the given TypeMirror. This form is used internally to avoid
     * infinite recursion in cases like `Enum<E extends Enum<E>>`. When we encounter such a
     * thing, we will make a TypeVariableName without bounds and add that to the `typeVariables`
     * map before looking up the bounds. Then if we encounter this TypeVariable again while
     * constructing the bounds, we can just return it from the map. And, the code that put the entry
     * in `variables` will make sure that the bounds are filled in before returning.
     */
    internal fun get(
      mirror: javax.lang.model.type.TypeVariable,
      typeVariables: MutableMap<TypeParameterElement, TypeVariableName>,
    ): TypeVariableName {
      val element = mirror.asElement() as TypeParameterElement
      var typeVariableName: TypeVariableName? = typeVariables[element]
      if (typeVariableName == null) {
        // Since the bounds field is public, we need to make it an unmodifiableList. But we control
        // the List that that wraps, which means we can change it before returning.
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        typeVariableName = TypeVariableName(element.simpleName.toString(), visibleBounds)
        typeVariables[element] = typeVariableName
        for (typeMirror in element.bounds) {
          bounds += get(typeMirror, typeVariables)
        }
        bounds.remove(ANY)
        bounds.remove(JAVA_OBJECT)
        if (bounds.isEmpty()) {
          bounds.add(NULLABLE_ANY)
        }
      }
      return typeVariableName
    }

    /** Returns type variable equivalent to `type`.  */
    internal fun get(
      type: java.lang.reflect.TypeVariable<*>,
      map: MutableMap<Type, TypeVariableName> = mutableMapOf(),
    ): TypeVariableName {
      var result: TypeVariableName? = map[type]
      if (result == null) {
        val bounds = mutableListOf<TypeName>()
        val visibleBounds = Collections.unmodifiableList(bounds)
        result = TypeVariableName(type.name, visibleBounds)
        map[type] = result
        for (bound in type.bounds) {
          bounds += get(bound, map)
        }
        bounds.remove(ANY)
        bounds.remove(JAVA_OBJECT)
        if (bounds.isEmpty()) {
          bounds.add(NULLABLE_ANY)
        }
      }
      return result
    }

    internal val NULLABLE_ANY_LIST = listOf(NULLABLE_ANY)
    private val JAVA_OBJECT = ClassName("java.lang", "Object")
  }
}

/** Returns type variable equivalent to `mirror`. */
@DelicateKotlinPoetApi(
  message = "Java reflection APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun TypeVariable.asTypeVariableName(): TypeVariableName =
  (asElement() as TypeParameterElement).asTypeVariableName()

/** Returns type variable equivalent to `element`. */
@DelicateKotlinPoetApi(
  message = "Element APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun TypeParameterElement.asTypeVariableName(): TypeVariableName {
  val name = simpleName.toString()
  val boundsTypeNames = bounds.map(TypeMirror::asTypeName)
    .ifEmpty(TypeVariableName.Companion::NULLABLE_ANY_LIST)
  return TypeVariableName.of(name, boundsTypeNames, variance = null)
}

public fun KTypeParameter.asTypeVariableName(): TypeVariableName {
  return asTypeVariableName(mutableMapOf())
}

/**
 * Internal method for resolving type parameters with cycle detection.
 * This is used to avoid infinite recursion when dealing with recursively bound generics.
 */
internal fun KTypeParameter.asTypeVariableName(map: MutableMap<KTypeParameter, TypeVariableName>): TypeVariableName {
  var result: TypeVariableName? = map[this]
  if (result == null) {
    val bounds = mutableListOf<TypeName>()
    val visibleBounds = Collections.unmodifiableList(bounds)
    result = TypeVariableName(
      name = name,
      bounds = visibleBounds,
      variance = when (variance) {
        KVariance.INVARIANT -> null
        KVariance.IN -> KModifier.IN
        KVariance.OUT -> KModifier.OUT
      },
    )
    map[this] = result

    for (bound in upperBounds) {
      bounds += bound.asTypeName(map)
    }

    bounds.remove(ANY)
    if (bounds.isEmpty()) {
      bounds.add(NULLABLE_ANY)
    }
  }
  return result
}
