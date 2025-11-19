/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.ACTUAL
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.PUBLIC
import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A generated typealias declaration */
public class TypeAliasSpec
private constructor(builder: Builder, private val tagMap: TagMap = builder.buildTagMap()) :
  Taggable by tagMap, Annotatable, Documentable {
  public val name: String = builder.name
  public val type: TypeName = builder.type
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  override val kdoc: CodeBlock = builder.kdoc.build()
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()

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

  public class Builder
  internal constructor(internal val name: String, internal val type: TypeName) :
    Taggable.Builder<Builder>, Annotatable.Builder<Builder>, Documentable.Builder<Builder> {
    public val modifiers: MutableSet<KModifier> = mutableSetOf()
    public val typeVariables: MutableSet<TypeVariableName> = mutableSetOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    override val annotations: MutableList<AnnotationSpec> = mutableListOf()

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

    // region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addAnnotation(annotationSpec: AnnotationSpec): Builder =
      super.addAnnotation(annotationSpec)

    @Suppress("RedundantOverride")
    override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder =
      super.addAnnotations(annotationSpecs)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: ClassName): Builder = super.addAnnotation(annotation)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    override fun addAnnotation(annotation: Class<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: KClass<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addKdoc(format: String, vararg args: Any): Builder = super.addKdoc(format, *args)

    @Suppress("RedundantOverride")
    override fun addKdoc(block: CodeBlock): Builder = super.addKdoc(block)

    // endregion

    public fun build(): TypeAliasSpec {
      for (it in modifiers) {
        require(it in ALLOWABLE_MODIFIERS) { "unexpected typealias modifier $it" }
      }
      return TypeAliasSpec(this)
    }

    private companion object {
      private val ALLOWABLE_MODIFIERS = setOf(PUBLIC, INTERNAL, PRIVATE, ACTUAL)
    }
  }

  public companion object {
    @JvmStatic public fun builder(name: String, type: TypeName): Builder = Builder(name, type)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    @JvmStatic
    public fun builder(name: String, type: Type): Builder = builder(name, type.asTypeName())

    @JvmStatic
    public fun builder(name: String, type: KClass<*>): Builder = builder(name, type.asTypeName())
  }
}
