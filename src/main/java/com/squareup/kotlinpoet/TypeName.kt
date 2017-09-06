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
@file:JvmName("TypeNames")

package com.squareup.kotlinpoet

import kotlin.reflect.KClass

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
 * Create new reference types like `com.example.HelloWorld` with [ClassName.get]. To build composite
 * types like `Set<Long>`, use the factory methods on [ParameterizedTypeName], [TypeVariableName],
 * and [WildcardTypeName].
 */
abstract class TypeName internal constructor(
    val nullable: Boolean, annotations: List<AnnotationSpec>) {
  val annotations = annotations.toImmutableList()

  /** Lazily-initialized toString of this type name.  */
  internal val cachedString: String by lazy {
    val resultBuilder = StringBuilder()
    val codeWriter = CodeWriter(resultBuilder)
    emitAnnotations(codeWriter)
    emit(codeWriter)
    if (nullable) resultBuilder.append("?")
    resultBuilder.toString()
  }

  fun annotated(vararg annotations: AnnotationSpec): TypeName {
    return annotated(annotations.toList())
  }

  abstract fun asNullable(): TypeName

  abstract fun asNonNullable(): TypeName

  abstract fun annotated(annotations: List<AnnotationSpec>): TypeName

  abstract fun withoutAnnotations(): TypeName

  val isAnnotated: Boolean
    get() = annotations.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int {
    return toString().hashCode()
  }

  override fun toString(): String {
    return cachedString
  }

  internal abstract fun emit(out: CodeWriter): CodeWriter

  @JvmName("emitAnnotations") internal fun emitAnnotations(out: CodeWriter) {
    for (annotation in annotations) {
      annotation.emit(out, true)
      out.emit(" ")
    }
  }

  @JvmName("emitNullable") internal fun emitNullable(out: CodeWriter) {
    if (nullable) {
      out.emit("?")
    }
  }
}

@JvmField val ANY = ClassName("kotlin", "Any")
@JvmField val ARRAY = ClassName("kotlin", "Array")
@JvmField val UNIT = Unit::class.asClassName()
@JvmField val BOOLEAN = ClassName("kotlin", "Boolean")
@JvmField val BYTE = ClassName("kotlin", "Byte")
@JvmField val SHORT = ClassName("kotlin", "Short")
@JvmField val INT = ClassName("kotlin", "Int")
@JvmField val LONG = ClassName("kotlin", "Long")
@JvmField val CHAR = ClassName("kotlin", "Char")
@JvmField val FLOAT = ClassName("kotlin", "Float")
@JvmField val DOUBLE = ClassName("kotlin", "Double")

/** Returns a [TypeName] equivalent to this [KClass].  */
@JvmName("get")
fun KClass<*>.asTypeName() = asClassName()
