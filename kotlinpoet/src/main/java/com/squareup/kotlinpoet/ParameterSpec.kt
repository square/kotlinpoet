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
import kotlin.reflect.KClass

/** A generated parameter declaration.  */
class ParameterSpec private constructor(
  builder: ParameterSpec.Builder,
  private val tagMap: TagMap = builder.buildTagMap()
) : Taggable by tagMap {
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val type = builder.type
  val defaultValue = builder.defaultValue

  constructor(name: String, type: TypeName, vararg modifiers: KModifier) : this(builder(name, type, *modifiers))
  constructor(name: String, type: TypeName, modifiers: Iterable<KModifier>) : this(builder(name, type, modifiers))

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

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString { emit(this) }

  fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.defaultValue = defaultValue
    builder.tags += tagMap.tags
    return builder
  }

  class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) : Taggable.Builder<ParameterSpec.Builder> {
    internal var defaultValue: CodeBlock? = null

    val kdoc = CodeBlock.builder()
    val annotations = mutableListOf<AnnotationSpec>()
    val modifiers = mutableListOf<KModifier>()
    override val tags = mutableMapOf<KClass<*>, Any>()

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
      annotations += annotationSpecs
    }

    fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
      annotations += annotationSpec
    }

    fun addAnnotation(annotation: ClassName) = apply {
      annotations += AnnotationSpec.builder(annotation).build()
    }

    fun addAnnotation(annotation: Class<*>) = addAnnotation(annotation.asClassName())

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(annotation.asClassName())

    fun addModifiers(vararg modifiers: KModifier) = apply {
      this.modifiers += modifiers
    }

    fun addModifiers(modifiers: Iterable<KModifier>) = apply {
      this.modifiers += modifiers
    }

    fun jvmModifiers(modifiers: Iterable<Modifier>) = apply {
      for (modifier in modifiers) {
        when (modifier) {
          Modifier.FINAL -> this.modifiers += KModifier.FINAL
          else -> throw IllegalArgumentException("unexpected parameter modifier $modifier")
        }
      }
    }

    fun defaultValue(format: String, vararg args: Any?) = defaultValue(CodeBlock.of(format, *args))

    fun defaultValue(codeBlock: CodeBlock) = apply {
      check(this.defaultValue == null) { "initializer was already set" }
      this.defaultValue = codeBlock
    }

    fun build() = ParameterSpec(this)
  }

  companion object {
    @JvmStatic fun get(element: VariableElement): ParameterSpec {
      val name = element.simpleName.toString()
      val type = element.asType().asTypeName()
      return ParameterSpec.builder(name, type)
          .jvmModifiers(element.modifiers)
          .build()
    }

    @JvmStatic fun parametersOf(method: ExecutableElement) =
        method.parameters.map { ParameterSpec.get(it) }

    @JvmStatic fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      return Builder(name, type).addModifiers(*modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, vararg modifiers: KModifier) =
        builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier) =
        builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun builder(name: String, type: TypeName, modifiers: Iterable<KModifier>): Builder {
      return Builder(name, type).addModifiers(modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, modifiers: Iterable<KModifier>) =
        builder(name, type.asTypeName(), modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, modifiers: Iterable<KModifier>) =
        builder(name, type.asTypeName(), modifiers)

    @JvmStatic fun unnamed(type: KClass<*>) = unnamed(type.asTypeName())

    @JvmStatic fun unnamed(type: Type) = unnamed(type.asTypeName())

    @JvmStatic fun unnamed(type: TypeName) = Builder("", type).build()
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
