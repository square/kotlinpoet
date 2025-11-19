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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.KModifier.CROSSINLINE
import com.squareup.kotlinpoet.KModifier.NOINLINE
import com.squareup.kotlinpoet.KModifier.VARARG
import java.lang.reflect.Type
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import kotlin.DeprecationLevel.ERROR
import kotlin.reflect.KClass

/** A generated parameter declaration. */
public class ParameterSpec
private constructor(builder: Builder, private val tagMap: TagMap = builder.buildTagMap()) :
  Taggable by tagMap, Annotatable, Documentable {
  public val name: String = builder.name
  override val kdoc: CodeBlock = builder.kdoc.build()
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val modifiers: Set<KModifier> =
    builder.modifiers
      .also {
        LinkedHashSet(it).apply {
          removeAll(ALLOWED_PARAMETER_MODIFIERS)
          if (!isEmpty()) {
            throw IllegalArgumentException(
              "Modifiers $this are not allowed on Kotlin parameters. Allowed modifiers: $ALLOWED_PARAMETER_MODIFIERS"
            )
          }
        }
      }
      .toImmutableSet()
  public val type: TypeName = builder.type
  public val defaultValue: CodeBlock? = builder.defaultValue

  public constructor(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
  ) : this(builder(name, type, *modifiers))

  public constructor(
    name: String,
    type: TypeName,
    modifiers: Iterable<KModifier>,
  ) : this(builder(name, type, modifiers))

  internal fun emit(
    codeWriter: CodeWriter,
    includeType: Boolean = true,
    inlineAnnotations: Boolean = true,
  ) {
    codeWriter.emitAnnotations(annotations, inlineAnnotations)
    codeWriter.emitModifiers(modifiers)
    if (name.isNotEmpty()) codeWriter.emitCode("%N", this)
    if (name.isNotEmpty() && includeType) codeWriter.emitCode(": ")
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

  public class Builder
  internal constructor(internal val name: String, internal val type: TypeName) :
    Taggable.Builder<Builder>, Annotatable.Builder<Builder>, Documentable.Builder<Builder> {
    internal var defaultValue: CodeBlock? = null

    public val modifiers: MutableList<KModifier> = mutableListOf()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val annotations: MutableList<AnnotationSpec> = mutableListOf()

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      this.modifiers += modifiers
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      this.modifiers += modifiers
    }

    @Deprecated(
      "There are no jvm modifiers applicable to parameters in Kotlin",
      ReplaceWith(""),
      level = ERROR,
    )
    public fun jvmModifiers(
      @Suppress("UNUSED_PARAMETER", "unused") modifiers: Iterable<Modifier>
    ): Builder = apply {
      throw IllegalArgumentException("JVM modifiers are not permitted on parameters in Kotlin")
    }

    public fun defaultValue(format: String, vararg args: Any?): Builder =
      defaultValue(CodeBlock.of(format, *args))

    public fun defaultValue(codeBlock: CodeBlock?): Builder = apply {
      this.defaultValue = codeBlock
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

    public fun build(): ParameterSpec = ParameterSpec(this)
  }

  public companion object {
    @DelicateKotlinPoetApi(
      message =
        "Element APIs don't give complete information on Kotlin types. Consider using" +
          " the kotlinpoet-metadata APIs instead."
    )
    @JvmStatic
    public fun get(element: VariableElement): ParameterSpec {
      val name = element.simpleName.toString()
      val type = element.asType().asTypeName()
      return builder(name, type).build()
    }

    @DelicateKotlinPoetApi(
      message =
        "Element APIs don't give complete information on Kotlin types. Consider using" +
          " the kotlinpoet-metadata APIs instead."
    )
    @JvmStatic
    public fun parametersOf(method: ExecutableElement): List<ParameterSpec> =
      method.parameters.map(::get)

    @JvmStatic
    public fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      return Builder(name, type).addModifiers(*modifiers)
    }

    @JvmStatic
    public fun builder(name: String, type: Type, vararg modifiers: KModifier): Builder =
      builder(name, type.asTypeName(), *modifiers)

    @JvmStatic
    public fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier): Builder =
      builder(name, type.asTypeName(), *modifiers)

    @JvmStatic
    public fun builder(name: String, type: TypeName, modifiers: Iterable<KModifier>): Builder {
      return Builder(name, type).addModifiers(modifiers)
    }

    @JvmStatic
    public fun builder(name: String, type: Type, modifiers: Iterable<KModifier>): Builder =
      builder(name, type.asTypeName(), modifiers)

    @JvmStatic
    public fun builder(name: String, type: KClass<*>, modifiers: Iterable<KModifier>): Builder =
      builder(name, type.asTypeName(), modifiers)

    @JvmStatic public fun unnamed(type: KClass<*>): ParameterSpec = unnamed(type.asTypeName())

    @JvmStatic public fun unnamed(type: Type): ParameterSpec = unnamed(type.asTypeName())

    @JvmStatic public fun unnamed(type: TypeName): ParameterSpec = Builder("", type).build()
  }
}

// From https://kotlinlang.org/spec/syntax-and-grammar.html#grammar-rule-parameterModifier
private val ALLOWED_PARAMETER_MODIFIERS = setOf(VARARG, NOINLINE, CROSSINLINE)

internal fun List<ParameterSpec>.emit(
  codeWriter: CodeWriter,
  forceNewLines: Boolean = false,
  emitBlock: (ParameterSpec) -> Unit = { it.emit(codeWriter) },
) =
  with(codeWriter) {
    emit("(")

    if (isNotEmpty()) {
      val emitNewLines = size > 2 || forceNewLines
      if (emitNewLines) {
        emit("\n")
        indent(1)
      }
      forEachIndexed { index, parameter ->
        if (index > 0) {
          emit(if (emitNewLines) "\n" else ", ")
        }
        emitBlock(parameter)
        if (emitNewLines) {
          emit(",")
        }
      }
      if (emitNewLines) {
        unindent(1)
        emit("\n")
      }
    }
    emit(")")
  }
