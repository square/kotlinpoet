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

import com.squareup.kotlinpoet.KModifier.ACTUAL
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A generated typealias declaration */
class TypeAliasSpec private constructor(
  builder: TypeAliasSpec.Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {
  val name = builder.name
  val type = builder.type
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val kdoc = builder.kdoc.build()

  internal fun emit(codeWriter: CodeWriter) {
    codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    codeWriter.emitModifiers(modifiers)
    codeWriter.emitCode("typealias %L", name)
    codeWriter.emitTypeVariables(typeVariables)
    codeWriter.emitCode(" = %T", type)
    codeWriter.emit("\n")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString { emit(this) }

  fun toBuilder(): Builder {
    val builder = Builder(name, type)
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.kdoc.add(kdoc)
    builder.tags += tagMap.tags
    return builder
  }

  class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) : Taggable.Builder<TypeAliasSpec.Builder> {
    internal val kdoc = CodeBlock.builder()

    val modifiers = mutableSetOf<KModifier>()
    val typeVariables = mutableSetOf<TypeVariableName>()
    override val tags = mutableMapOf<KClass<*>, Any>()

    init {
      require(name.isName) { "not a valid name: $name" }
    }

    fun addModifiers(vararg modifiers: KModifier) = apply {
      modifiers.forEach(this::addModifier)
    }

    private fun addModifier(modifier: KModifier) {
      this.modifiers.add(modifier)
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun build(): TypeAliasSpec {
      for (it in modifiers) {
        require(it in ALLOWABLE_MODIFIERS) {
          "unexpected typealias modifier $it"
        }
      }
      return TypeAliasSpec(this)
    }

    private companion object {
      private val ALLOWABLE_MODIFIERS = setOf(PUBLIC, INTERNAL, PRIVATE, ACTUAL)
    }
  }

  companion object {
    @JvmStatic fun builder(name: String, type: TypeName) = Builder(name, type)

    @JvmStatic fun builder(name: String, type: Type) = builder(name, type.asTypeName())

    @JvmStatic fun builder(name: String, type: KClass<*>) = builder(name, type.asTypeName())
  }
}
