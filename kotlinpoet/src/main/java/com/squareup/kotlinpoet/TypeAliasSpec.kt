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
public class TypeAliasSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {
  public val name: String = builder.name
  public val type: TypeName = builder.type
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  public val kdoc: CodeBlock = builder.kdoc.build()
  public val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()

  internal fun emit(codeWriter: CodeWriter) {
    codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, setOf(PUBLIC))
    codeWriter.emitCode("typealias %N", name)
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

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString { emit(this) }

  @JvmOverloads
  public fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type)
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.tags += tagMap.tags
    return builder
  }

  public class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) : Taggable.Builder<Builder> {
    internal val kdoc = CodeBlock.builder()

    public val modifiers: MutableSet<KModifier> = mutableSetOf()
    public val typeVariables: MutableSet<TypeVariableName> = mutableSetOf()
    public val annotations: MutableList<AnnotationSpec> = mutableListOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      modifiers.forEach(this::addModifier)
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      modifiers.forEach(this::addModifier)
    }

    private fun addModifier(modifier: KModifier) {
      this.modifiers.add(modifier)
    }

    public fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder = apply {
      this.typeVariables += typeVariables
    }

    public fun addTypeVariable(typeVariable: TypeVariableName): Builder = apply {
      typeVariables += typeVariable
    }

    public fun addKdoc(format: String, vararg args: Any): Builder = apply {
      kdoc.add(format, *args)
    }

    public fun addKdoc(block: CodeBlock): Builder = apply {
      kdoc.add(block)
    }

    public fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder = apply {
      this.annotations += annotationSpecs
    }

    public fun addAnnotation(annotationSpec: AnnotationSpec): Builder = apply {
      annotations += annotationSpec
    }

    public fun addAnnotation(annotation: ClassName): Builder = apply {
      annotations += AnnotationSpec.builder(annotation).build()
    }

    public fun addAnnotation(annotation: Class<*>): Builder =
        addAnnotation(annotation.asClassName())

    public fun addAnnotation(annotation: KClass<*>): Builder =
        addAnnotation(annotation.asClassName())

    public fun build(): TypeAliasSpec {
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

  public companion object {
    @JvmStatic public fun builder(name: String, type: TypeName): Builder = Builder(name, type)

    @JvmStatic public fun builder(name: String, type: Type): Builder =
        builder(name, type.asTypeName())

    @JvmStatic public fun builder(name: String, type: KClass<*>): Builder =
        builder(name, type.asTypeName())
  }
}
