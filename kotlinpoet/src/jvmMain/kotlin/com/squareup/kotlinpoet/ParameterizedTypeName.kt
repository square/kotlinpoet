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
@file:JvmName("ParameterizedTypeNames")

package com.squareup.kotlinpoet

import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

public class ParameterizedTypeName internal constructor(
  private val enclosingType: TypeName?,
  public val rawType: ClassName,
  typeArguments: List<TypeName>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList(),
  tags: Map<KClass<*>, Any> = emptyMap(),
) : TypeName(nullable, annotations, TagMap(tags)) {
  public val typeArguments: List<TypeName> = typeArguments.toImmutableList()

  init {
    require(typeArguments.isNotEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>,
  ): ParameterizedTypeName {
    return ParameterizedTypeName(enclosingType, rawType, typeArguments, nullable, annotations, tags)
  }

  public fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations,
    tags: Map<KClass<*>, Any> = this.tags,
    typeArguments: List<TypeName> = this.typeArguments,
  ): ParameterizedTypeName {
    return ParameterizedTypeName(enclosingType, rawType, typeArguments, nullable, annotations, tags)
  }

  public fun plusParameter(typeArgument: TypeName): ParameterizedTypeName =
    ParameterizedTypeName(
      enclosingType,
      rawType,
      typeArguments + typeArgument,
      isNullable,
      annotations,
    )

  public fun plusParameter(typeArgument: KClass<*>): ParameterizedTypeName =
    plusParameter(typeArgument.asClassName())

  public fun plusParameter(typeArgument: Class<*>): ParameterizedTypeName =
    plusParameter(typeArgument.asClassName())

  override fun emit(out: CodeWriter): CodeWriter {
    if (enclosingType != null) {
      enclosingType.emitAnnotations(out)
      enclosingType.emit(out)
      out.emit("." + rawType.simpleName)
    } else {
      rawType.emitAnnotations(out)
      rawType.emit(out)
    }
    if (typeArguments.isNotEmpty()) {
      out.emit("<")
      typeArguments.forEachIndexed { index, parameter ->
        if (index > 0) out.emit(", ")
        parameter.emitAnnotations(out)
        parameter.emit(out)
        parameter.emitNullable(out)
      }
      out.emit(">")
    }
    return out
  }

  /**
   * Returns a new [ParameterizedTypeName] instance for the specified `name` as nested inside this
   * class, with the specified `typeArguments`.
   */
  public fun nestedClass(name: String, typeArguments: List<TypeName>): ParameterizedTypeName =
    ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ParameterizedTypeName

    if (enclosingType != other.enclosingType) return false
    if (rawType != other.rawType) return false
    if (typeArguments != other.typeArguments) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (enclosingType?.hashCode() ?: 0)
    result = 31 * result + rawType.hashCode()
    result = 31 * result + typeArguments.hashCode()
    return result
  }

  public companion object {
    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun ClassName.parameterizedBy(
      vararg typeArguments: TypeName,
    ): ParameterizedTypeName = ParameterizedTypeName(null, this, typeArguments.toList())

    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun KClass<*>.parameterizedBy(
      vararg typeArguments: KClass<*>,
    ): ParameterizedTypeName =
      ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun Class<*>.parameterizedBy(
      vararg typeArguments: Type,
    ): ParameterizedTypeName =
      ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun ClassName.parameterizedBy(
      typeArguments: List<TypeName>,
    ): ParameterizedTypeName = ParameterizedTypeName(null, this, typeArguments)

    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun KClass<*>.parameterizedBy(
      typeArguments: Iterable<KClass<*>>,
    ): ParameterizedTypeName =
      ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun Class<*>.parameterizedBy(
      typeArguments: Iterable<Type>,
    ): ParameterizedTypeName =
      ParameterizedTypeName(null, asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArgument` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun ClassName.plusParameter(
      typeArgument: TypeName,
    ): ParameterizedTypeName = parameterizedBy(typeArgument)

    /** Returns a parameterized type, applying `typeArgument` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun KClass<*>.plusParameter(
      typeArgument: KClass<*>,
    ): ParameterizedTypeName = parameterizedBy(typeArgument)

    /** Returns a parameterized type, applying `typeArgument` to `this`. */
    @JvmStatic
    @JvmName("get")
    public fun Class<*>.plusParameter(
      typeArgument: Class<*>,
    ): ParameterizedTypeName = parameterizedBy(typeArgument)

    /** Returns a parameterized type equivalent to `type`. */
    internal fun get(
      type: ParameterizedType,
      map: MutableMap<Type, TypeVariableName>,
    ): ParameterizedTypeName {
      val rawType = (type.rawType as Class<*>).asClassName()
      val ownerType = if (type.ownerType is ParameterizedType &&
        !Modifier.isStatic((type.rawType as Class<*>).modifiers)
      ) {
        type.ownerType as ParameterizedType
      } else {
        null
      }

      val typeArguments = type.actualTypeArguments.map { get(it, map = map) }
      return if (ownerType != null) {
        get(ownerType, map = map).nestedClass(rawType.simpleName, typeArguments)
      } else {
        ParameterizedTypeName(null, rawType, typeArguments)
      }
    }

    /** Returns a type name equivalent to type with given list of type arguments. */
    internal fun get(
      type: KClass<*>,
      nullable: Boolean,
      typeArguments: List<KTypeProjection>,
      map: MutableMap<KTypeParameter, TypeVariableName> = mutableMapOf(),
    ): TypeName {
      if (typeArguments.isEmpty()) {
        return type.asTypeName().run { if (nullable) copy(nullable = true) else this }
      }

      val effectiveType = if (type.java.isArray) Array<Unit>::class else type
      val enclosingClass = type.java.enclosingClass?.kotlin

      return ParameterizedTypeName(
        enclosingClass?.let {
          get(it, false, typeArguments.drop(effectiveType.typeParameters.size), map)
        },
        effectiveType.asTypeName(),
        typeArguments.take(effectiveType.typeParameters.size).map { (paramVariance, paramType) ->
          val typeName = paramType?.asTypeName(map) ?: return@map STAR
          when (paramVariance) {
            null -> STAR
            KVariance.INVARIANT -> typeName
            KVariance.IN -> WildcardTypeName.consumerOf(typeName)
            KVariance.OUT -> WildcardTypeName.producerOf(typeName)
          }
        },
        nullable,
        effectiveType.annotations.map { AnnotationSpec.get(it) },
      )
    }
  }
}

/** Returns a parameterized type equivalent to `type`.  */
@DelicateKotlinPoetApi(
  message = "Java reflection APIs don't give complete information on Kotlin types. Consider " +
    "using the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun ParameterizedType.asParameterizedTypeName(): ParameterizedTypeName =
  ParameterizedTypeName.get(this, mutableMapOf())

/**
 * Returns a [TypeName] equivalent to the given Kotlin KType using reflection, maybe using kotlin-reflect
 * if required.
 */
public fun KType.asTypeName(): TypeName = asTypeName(mutableMapOf())

/**
 * Internal method for resolving KType with cycle detection.
 * This is used to avoid infinite recursion when dealing with recursively bound generics.
 */
internal fun KType.asTypeName(map: MutableMap<KTypeParameter, TypeVariableName>): TypeName {
  val classifier = this.classifier
  if (classifier is KTypeParameter) {
    return classifier.asTypeVariableName(map).run { if (isMarkedNullable) copy(nullable = true) else this }
  }

  if (classifier == null || classifier !is KClass<*>) {
    throw IllegalArgumentException("Cannot build TypeName for $this")
  }

  return ParameterizedTypeName.get(classifier, this.isMarkedNullable, this.arguments, map)
}
