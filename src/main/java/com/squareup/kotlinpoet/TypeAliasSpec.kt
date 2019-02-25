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
class TypeAliasSpec private constructor(builder: TypeAliasSpec.Builder) {
  val name = builder.name
  val type = builder.type
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val kdoc = builder.kdoc.build()
  private val tags: Map<KClass<*>, Any> = builder.tags.toImmutableMap()

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: Class<out T>): T? = tag(type.kotlin)

  /** Returns the tag attached with [type] as a key, or null if no tag is attached with that key. */
  fun <T : Any> tag(type: KClass<out T>): T? {
    @Suppress("UNCHECKED_CAST")
    return tags[type] as? T
  }

  /** Returns the tag attached with [T] as a key, or null if no tag is attached with that key. */
  inline fun <reified T : Any> tag(): T? = tag(T::class)

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
    builder.tags += tags
    return builder
  }

  class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) {
    internal val kdoc = CodeBlock.builder()

    val modifiers = mutableSetOf<KModifier>()
    val typeVariables = mutableSetOf<TypeVariableName>()
    val tags = mutableMapOf<KClass<*>, Any>()

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

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a
     * request using [TypeAliasSpec.tag]. Use `null` to remove any existing tag assigned for
     * [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    fun <T : Any> tag(type: Class<out T>, tag: T?) = tag(type.kotlin, tag)

    /**
     * Attaches [tag] to the request using [type] as a key. Tags can be read from a
     * request using [TypeAliasSpec.tag]. Use `null` to remove any existing tag assigned for
     * [type].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    fun <T : Any> tag(type: KClass<out T>, tag: T?) = apply {
      if (tag == null) {
        this.tags.remove(type)
      } else {
        this.tags[type] = tag
      }
    }

    /**
     * Attaches [tag] to the request using [T] as a key. Tags can be read from a
     * request using [TypeAliasSpec.tag]. Use `null` to remove any existing tag assigned for
     * [T].
     *
     * Use this API to attach originating elements, debugging, or other application data to a spec
     * so that you may read it in other APIs or callbacks.
     */
    inline fun <reified T : Any> tag(tag: T?) = tag(T::class, tag)

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
