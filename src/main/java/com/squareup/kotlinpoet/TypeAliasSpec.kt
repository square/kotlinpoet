/*
 * Copyright (C) 2017 Square, Inc.
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
import java.io.StringWriter
import java.lang.reflect.Type
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass

/** A generated typealias declaration */
class TypeAliasSpec private constructor(builder: TypeAliasSpec.Builder) {
  val name: String = builder.name
  val type: TypeName = builder.type
  val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter) {
    codeWriter.emitModifiers(modifiers)
    codeWriter.emitCode("typealias %L = %T", name, type)
    codeWriter.emit("\n")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String {
    val out = StringWriter()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(name, type)
    builder.modifiers.addAll(modifiers)
    return builder
  }

  class Builder internal constructor(
      internal val name: String,
      internal val type: TypeName) {
    internal var modifiers: MutableSet<KModifier> = mutableSetOf()

    init {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
    }

    fun visibility(modifier: KModifier): Builder = apply {
      require(modifier == KModifier.PUBLIC || modifier == KModifier.INTERNAL
          || modifier == KModifier.PRIVATE) {
        "unexpected typealias modifier $modifier"
      }
      this.modifiers.add(modifier)
    }

    fun build() = TypeAliasSpec(this)
  }

  companion object {

    @JvmStatic fun builder(name: String, type: TypeName) = Builder(name, type)

    @JvmStatic fun builder(name: String, type: Type) = builder(name, TypeName.get(type))

    @JvmStatic fun builder(name: String, type: KClass<*>) = builder(name, TypeName.get(type))
  }
}