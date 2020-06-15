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
package com.squareup.kotlinpoet

import java.lang.reflect.Type
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import kotlin.DeprecationLevel.WARNING
import kotlin.reflect.KClass

/** A generated parameter declaration. */
public class ParameterSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {
  public val name: String = builder.name
  public val kdoc: CodeBlock = builder.kdoc.build()
  public val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val type: TypeName = builder.type
  public val defaultValue: CodeBlock? = builder.defaultValue

  public constructor(name: String, type: TypeName, vararg modifiers: KModifier) :
      this(builder(name, type, *modifiers))
  public constructor(name: String, type: TypeName, modifiers: Iterable<KModifier>) :
      this(builder(name, type, modifiers))

  internal fun emit(
    codeWriter: CodeWriter,
    includeType: Boolean = true,
    emitKdoc: Boolean = false,
    inlineAnnotations: Boolean = true
  ) {
    if (emitKdoc) codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    codeWriter.emitAnnotations(annotations, inlineAnnotations)
    codeWriter.emitModifiers(modifiers)
    if (name.isNotEmpty()) codeWriter.emitCode("%N", this)
    if (name.isNotEmpty() && includeType) codeWriter.emitCode(":·")
    if (includeType) codeWriter.emitCode("%T", type)
    emitDefaultValue(codeWriter)
  }

  internal fun emitDefaultValue(codeWriter: CodeWriter) {
    if (defaultValue != null) {
      codeWriter.emitCode(if (defaultValue.hasStatements()) " = %L" else " = «%L»", defaultValue)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString { emit(this) }

  public fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.defaultValue = defaultValue
    builder.tags += tagMap.tags
    return builder
  }

  public class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) : Taggable.Builder<Builder> {
    internal var defaultValue: CodeBlock? = null

    public val kdoc: CodeBlock.Builder = CodeBlock.builder()
    public val annotations: MutableList<AnnotationSpec> = mutableListOf()
    public val modifiers: MutableList<KModifier> = mutableListOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    public fun addKdoc(format: String, vararg args: Any): Builder = apply {
      kdoc.add(format, *args)
    }

    public fun addKdoc(block: CodeBlock): Builder = apply {
      kdoc.add(block)
    }

    public fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder = apply {
      annotations += annotationSpecs
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

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      this.modifiers += modifiers
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      this.modifiers += modifiers
    }

    public fun jvmModifiers(modifiers: Iterable<Modifier>): Builder = apply {
      for (modifier in modifiers) {
        when (modifier) {
          Modifier.FINAL -> this.modifiers += KModifier.FINAL
          else -> throw IllegalArgumentException("unexpected parameter modifier $modifier")
        }
      }
    }

    public fun defaultValue(format: String, vararg args: Any?): Builder =
        defaultValue(CodeBlock.of(format, *args))

    public fun defaultValue(codeBlock: CodeBlock): Builder = apply {
      check(this.defaultValue == null) { "initializer was already set" }
      this.defaultValue = codeBlock
    }

    public fun build(): ParameterSpec = ParameterSpec(this)
  }

  public companion object {
    @Deprecated(
        message = "Element APIs don't give complete information on Kotlin types. Consider using" +
            " the kotlinpoet-metadata APIs instead.",
        level = WARNING
    )
    @JvmStatic
    public fun get(element: VariableElement): ParameterSpec {
      val name = element.simpleName.toString()
      val type = element.asType().asTypeName()
      return builder(name, type)
          .jvmModifiers(element.modifiers)
          .build()
    }

    @Deprecated(
        message = "Element APIs don't give complete information on Kotlin types. Consider using" +
            " the kotlinpoet-metadata APIs instead.",
        level = WARNING
    )
    @JvmStatic
    public fun parametersOf(method: ExecutableElement): List<ParameterSpec> =
        method.parameters.map(::get)

    @JvmStatic public fun builder(
      name: String,
      type: TypeName,
      vararg modifiers: KModifier
    ): Builder {
      return Builder(name, type).addModifiers(*modifiers)
    }

    @JvmStatic public fun builder(name: String, type: Type, vararg modifiers: KModifier): Builder =
        builder(name, type.asTypeName(), *modifiers)

    @JvmStatic public fun builder(
      name: String,
      type: KClass<*>,
      vararg modifiers: KModifier
    ): Builder = builder(name, type.asTypeName(), *modifiers)

    @JvmStatic public fun builder(
      name: String,
      type: TypeName,
      modifiers: Iterable<KModifier>
    ): Builder {
      return Builder(name, type).addModifiers(modifiers)
    }

    @JvmStatic public fun builder(
      name: String,
      type: Type,
      modifiers: Iterable<KModifier>
    ): Builder = builder(name, type.asTypeName(), modifiers)

    @JvmStatic public fun builder(
      name: String,
      type: KClass<*>,
      modifiers: Iterable<KModifier>
    ): Builder = builder(name, type.asTypeName(), modifiers)

    @JvmStatic public fun unnamed(type: KClass<*>): ParameterSpec = unnamed(type.asTypeName())

    @JvmStatic public fun unnamed(type: Type): ParameterSpec = unnamed(type.asTypeName())

    @JvmStatic public fun unnamed(type: TypeName): ParameterSpec = Builder("", type).build()
  }
}

internal fun List<ParameterSpec>.emit(
  codeWriter: CodeWriter,
  forceNewLines: Boolean = false,
  forceParensOnEmpty: Boolean = true,
  emitBlock: (ParameterSpec) -> Unit = { it.emit(codeWriter) }
) = with(codeWriter) {
  val emitParens = isNotEmpty() || forceParensOnEmpty
  if (emitParens) {
    emit("(")
  }
  if (size > 0) {
    val emitNewLines = size > 2 || forceNewLines
    if (emitNewLines) {
      emit("\n")
      indent(1)
    }
    val delimiter = if (emitNewLines) ",\n" else ", "
    forEachIndexed { index, parameter ->
      if (index > 0) emit(delimiter)
      emitBlock(parameter)
    }
    if (emitNewLines) {
      unindent(1)
      emit("\n")
    }
  }
  if (emitParens) {
    emit(")")
  }
}
