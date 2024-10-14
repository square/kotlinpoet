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
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.jvm.alias.JvmClass
import com.squareup.kotlinpoet.jvm.alias.JvmType
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection

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

  public fun plusParameter(typeArgument: JvmClass<*>): ParameterizedTypeName =
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
        if (index > 0) out.emit(",·")
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
    if (other !is ParameterizedTypeName) return false
    if (!super.equals(other)) return false

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
    public fun JvmClass<*>.parameterizedBy(
      vararg typeArguments: JvmType,
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
    public fun JvmClass<*>.parameterizedBy(
      typeArguments: Iterable<JvmType>,
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
    public fun JvmClass<*>.plusParameter(
      typeArgument: JvmClass<*>,
    ): ParameterizedTypeName = parameterizedBy(typeArgument)
  }
}

internal expect fun ParameterizedTypeName.Companion.get(
  type: KClass<*>,
  nullable: Boolean,
  typeArguments: List<KTypeProjection>,
): TypeName

/**
 * Returns a [TypeName] equivalent to the given Kotlin KType using reflection, maybe using kotlin-reflect
 * if required.
 */
public fun KType.asTypeName(): TypeName {
  val classifier = this.classifier
  if (classifier is KTypeParameter) {
    return classifier.asTypeVariableName().run { if (isMarkedNullable) copy(nullable = true) else this }
  }

  if (classifier == null || classifier !is KClass<*>) {
    throw IllegalArgumentException("Cannot build TypeName for $this")
  }

  return ParameterizedTypeName.get(classifier, this.isMarkedNullable, this.arguments)
}
