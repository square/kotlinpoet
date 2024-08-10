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
@file:JvmName("TypeNames")
// @file:JvmMultifileClass
// Can't use JvmMultifileClass because of
// err: JvmField can't be applied to top level property of a file annotated with JvmMultifileClass

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.jvm.alias.JvmType
import com.squareup.kotlinpoet.jvm.alias.JvmTypeMirror
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * Any type in Kotlin's type system. This class identifies simple types like `Int` and `String`,
 * nullable types like `Int?`, composite types like `Array<String>` and `Set<String>`, and
 * unassignable types like `Unit`.
 *
 * Type names are dumb identifiers only and do not model the values they name. For example, the
 * type name for `kotlin.List` doesn't know about the `size()` function, the fact that lists are
 * collections, or even that it accepts a single type parameter.
 *
 * Instances of this class are immutable value objects that implement `equals()` and `hashCode()`
 * properly.
 *
 * Referencing existing types
 * --------------------------
 *
 * In an annotation processor you can get a type name instance for a type mirror by calling
 * [asTypeName]. In reflection code, you can use [asTypeName].

 * Defining new types
 * ------------------
 *
 * Create new reference types like `com.example.HelloWorld` with [ClassName.bestGuess]. To build composite
 * types like `Set<Long>`, use the factory methods on [ParameterizedTypeName], [TypeVariableName],
 * and [WildcardTypeName].
 */
public sealed class TypeName constructor(
  public val isNullable: Boolean,
  annotations: List<AnnotationSpec>,
  internal val tagMap: TagMap,
) : Taggable by tagMap, Annotatable {
  override val annotations: List<AnnotationSpec> = annotations.toImmutableList()

  /** Lazily-initialized toString of this type name.  */
  private val cachedString: String by lazy {
    buildCodeString {
      emitAnnotations(this)
      emit(this)
      if (isNullable) emit("?")
    }
  }

  public fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
  ): TypeName {
    return copy(nullable, annotations, this.tags)
  }

  public abstract fun copy(
    nullable: Boolean = this.isNullable,
    annotations: List<AnnotationSpec> = this.annotations.toList(),
    tags: Map<KClass<*>, Any> = this.tags,
  ): TypeName

  public val isAnnotated: Boolean get() = annotations.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is TypeName) return false

    if (isNullable != other.isNullable) return false
    if (annotations != other.annotations) return false
    // do not check for equality of tags, these are considered side-channel data

    return true
  }

  override fun hashCode(): Int {
    var result = isNullable.hashCode()
    result = 31 * result + annotations.hashCode()
    return result
  }

  override fun toString(): String = cachedString

  internal abstract fun emit(out: CodeWriter): CodeWriter

  internal fun emitAnnotations(out: CodeWriter) {
    for (annotation in annotations) {
      annotation.emit(out, true)
      out.emit(" ")
    }
  }

  internal fun emitNullable(out: CodeWriter) {
    if (isNullable) {
      out.emit("?")
    }
  }

  public companion object {
  }
}

@JvmField public val ANY: ClassName = ClassName("kotlin", "Any")

@JvmField public val ARRAY: ClassName = ClassName("kotlin", "Array")

@JvmField public val UNIT: ClassName = ClassName("kotlin", "Unit")

@JvmField public val BOOLEAN: ClassName = ClassName("kotlin", "Boolean")

@JvmField public val BYTE: ClassName = ClassName("kotlin", "Byte")

@JvmField public val SHORT: ClassName = ClassName("kotlin", "Short")

@JvmField public val INT: ClassName = ClassName("kotlin", "Int")

@JvmField public val LONG: ClassName = ClassName("kotlin", "Long")

@JvmField public val CHAR: ClassName = ClassName("kotlin", "Char")

@JvmField public val FLOAT: ClassName = ClassName("kotlin", "Float")

@JvmField public val DOUBLE: ClassName = ClassName("kotlin", "Double")

@JvmField public val STRING: ClassName = ClassName("kotlin", "String")

@JvmField public val CHAR_SEQUENCE: ClassName = ClassName("kotlin", "CharSequence")

@JvmField public val COMPARABLE: ClassName = ClassName("kotlin", "Comparable")

@JvmField public val THROWABLE: ClassName = ClassName("kotlin", "Throwable")

@JvmField public val ANNOTATION: ClassName = ClassName("kotlin", "Annotation")

@JvmField public val NOTHING: ClassName = ClassName("kotlin", "Nothing")

@JvmField public val NUMBER: ClassName = ClassName("kotlin", "Number")

@JvmField public val ITERABLE: ClassName = ClassName("kotlin.collections", "Iterable")

@JvmField public val COLLECTION: ClassName = ClassName("kotlin.collections", "Collection")

@JvmField public val LIST: ClassName = ClassName("kotlin.collections", "List")

@JvmField public val SET: ClassName = ClassName("kotlin.collections", "Set")

@JvmField public val MAP: ClassName = ClassName("kotlin.collections", "Map")

@JvmField public val MAP_ENTRY: ClassName = MAP.nestedClass("Entry")

@JvmField public val MUTABLE_ITERABLE: ClassName =
  ClassName("kotlin.collections", "MutableIterable")

@JvmField public val MUTABLE_COLLECTION: ClassName =
  ClassName("kotlin.collections", "MutableCollection")

@JvmField public val MUTABLE_LIST: ClassName = ClassName("kotlin.collections", "MutableList")

@JvmField public val MUTABLE_SET: ClassName = ClassName("kotlin.collections", "MutableSet")

@JvmField public val MUTABLE_MAP: ClassName = ClassName("kotlin.collections", "MutableMap")

@JvmField public val MUTABLE_MAP_ENTRY: ClassName = MUTABLE_MAP.nestedClass("Entry")

@JvmField public val BOOLEAN_ARRAY: ClassName = ClassName("kotlin", "BooleanArray")

@JvmField public val BYTE_ARRAY: ClassName = ClassName("kotlin", "ByteArray")

@JvmField public val CHAR_ARRAY: ClassName = ClassName("kotlin", "CharArray")

@JvmField public val SHORT_ARRAY: ClassName = ClassName("kotlin", "ShortArray")

@JvmField public val INT_ARRAY: ClassName = ClassName("kotlin", "IntArray")

@JvmField public val LONG_ARRAY: ClassName = ClassName("kotlin", "LongArray")

@JvmField public val FLOAT_ARRAY: ClassName = ClassName("kotlin", "FloatArray")

@JvmField public val DOUBLE_ARRAY: ClassName = ClassName("kotlin", "DoubleArray")

@JvmField public val ENUM: ClassName = ClassName("kotlin", "Enum")

@JvmField public val U_BYTE: ClassName = ClassName("kotlin", "UByte")

@JvmField public val U_SHORT: ClassName = ClassName("kotlin", "UShort")

@JvmField public val U_INT: ClassName = ClassName("kotlin", "UInt")

@JvmField public val U_LONG: ClassName = ClassName("kotlin", "ULong")

@JvmField public val U_BYTE_ARRAY: ClassName = ClassName("kotlin", "UByteArray")

@JvmField public val U_SHORT_ARRAY: ClassName = ClassName("kotlin", "UShortArray")

@JvmField public val U_INT_ARRAY: ClassName = ClassName("kotlin", "UIntArray")

@JvmField public val U_LONG_ARRAY: ClassName = ClassName("kotlin", "ULongArray")

/** The wildcard type `*` which is shorthand for `out Any?`. */
@JvmField public val STAR: WildcardTypeName = WildcardTypeName.producerOf(ANY.copy(nullable = true))

/** [Dynamic] is a singleton `object` type, so this is a shorthand for it in Java. */
@JvmField public val DYNAMIC: Dynamic = Dynamic

/** Returns a [TypeName] equivalent to this [JvmTypeMirror]. */
@DelicateKotlinPoetApi(
  message = "Mirror APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
@JvmName("get")
public fun JvmTypeMirror.asTypeName(): TypeName = asTypeNameInternal()

/** Returns a [TypeName] equivalent to this [JvmType].  */
@JvmName("get")
public fun JvmType.asTypeName(): TypeName = asTypeNameInternal()

/** Returns a [TypeName] equivalent to this [KClass].  */
@JvmName("get")
public fun KClass<*>.asTypeName(): ClassName = asClassName()

internal expect fun JvmTypeMirror.asTypeNameInternal(): TypeName

/** Returns a [TypeName] equivalent to this [JvmType].  */
internal expect fun JvmType.asTypeNameInternal(): TypeName

/**
 * Returns a [TypeName] equivalent of the reified type parameter [T] using reflection, maybe using kotlin-reflect
 * if required.
 */
public inline fun <reified T> typeNameOf(): TypeName = typeOf<T>().asTypeName()
