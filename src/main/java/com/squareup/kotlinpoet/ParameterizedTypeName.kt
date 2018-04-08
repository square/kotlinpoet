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
@file:JvmName("ParameterizedTypeNames")

package com.squareup.kotlinpoet

import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.lang.model.util.Types
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection

class ParameterizedTypeName internal constructor(
  private val enclosingType: TypeName?,
  val rawType: ClassName,
  typeArguments: List<TypeName>,
  nullable: Boolean = false,
  annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  val typeArguments = typeArguments.toImmutableList()

  init {
    require(typeArguments.isNotEmpty() || enclosingType != null) {
      "no type arguments: $rawType"
    }
  }

  override fun asNullable()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, true, annotations)

  override fun asNonNullable()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>) = ParameterizedTypeName(
      enclosingType, rawType, typeArguments, nullable, this.annotations + annotations)

  override fun withoutAnnotations()
      = ParameterizedTypeName(enclosingType, rawType, typeArguments, nullable)

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
  fun nestedClass(name: String, typeArguments: List<TypeName>)
      = ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments)

  companion object {
    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: ClassName, vararg typeArguments: TypeName)
        = ParameterizedTypeName(null, rawType, typeArguments.toList())

    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: KClass<*>, vararg typeArguments: KClass<*>)
        = ParameterizedTypeName(null, rawType.asClassName(),
        typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type, applying `typeArguments` to `rawType`.  */
    @JvmStatic fun get(rawType: Class<*>, vararg typeArguments: Type) = ParameterizedTypeName(
        null, rawType.asClassName(), typeArguments.map { it.asTypeName() })

    /** Returns a parameterized type equivalent to `type`.  */
    internal fun get(
      type: ParameterizedType,
      map: MutableMap<Type, TypeVariableName>
    ): ParameterizedTypeName {
      val rawType = (type.rawType as Class<*>).asClassName()
      val ownerType = if (type.ownerType is ParameterizedType
          && !Modifier.isStatic((type.rawType as Class<*>).modifiers))
        type.ownerType as ParameterizedType else
        null

      val typeArguments = type.actualTypeArguments.map { TypeName.get(it, map = map) }
      return if (ownerType != null)
        get(ownerType, map = map).nestedClass(rawType.simpleName, typeArguments) else
        ParameterizedTypeName(null, rawType, typeArguments)
    }

    /** Returns a type name equivalent to type with given list of type arguments.  */
    internal fun get(type: KClass<*>, nullable: Boolean, typeArguments: List<KTypeProjection>): TypeName {
      if (typeArguments.isEmpty()) {
        return type.asTypeName()
      }

      val enclosingClass = type.java.enclosingClass?.kotlin

      return ParameterizedTypeName(
          enclosingClass?.let {
            get(it, false, typeArguments.drop(type.typeParameters.size))
          },
          type.asTypeName(),
          typeArguments.take(type.typeParameters.size).map {
            it.type?.asTypeName() ?: WildcardTypeName.subtypeOf(ANY)
          },
          nullable,
          type.annotations.map { AnnotationSpec.get(it) })
    }
  }
}

/** Returns a parameterized type equivalent to `type`.  */
@JvmName("get")
fun ParameterizedType.asParameterizedTypeName() = ParameterizedTypeName.get(this, mutableMapOf())

/** Returns a class name equivalent to given Kotlin KType.  */
fun KType.asTypeName(): TypeName {
  val classifier = this.classifier
  if (classifier is KTypeParameter) {
    return classifier.asTypeVariableName()
  }

  if (classifier == null || classifier !is KClass<*>) {
    throw IllegalArgumentException("Cannot build TypeName for $this")
  }

  return ParameterizedTypeName.get(classifier, this.isMarkedNullable, this.arguments)
}
