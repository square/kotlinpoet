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

import com.squareup.kotlinpoet.FunSpec.Companion.GETTER
import com.squareup.kotlinpoet.FunSpec.Companion.SETTER
import com.squareup.kotlinpoet.KModifier.Target.PROPERTY
import java.lang.reflect.Type
import javax.lang.model.element.Element
import kotlin.reflect.KClass

/** A generated property declaration.  */
class PropertySpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
  private val delegateOriginatingElementsHolder: OriginatingElementsHolder = builder.buildOriginatingElements()
) : Taggable by tagMap, OriginatingElementsHolder by delegateOriginatingElementsHolder {
  val mutable = builder.mutable
  val name = builder.name
  val type = builder.type
  val kdoc = builder.kdoc.build()
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val initializer = builder.initializer
  val delegated = builder.delegated
  val getter = builder.getter
  val setter = builder.setter
  val receiverType = builder.receiverType

  init {
    require(typeVariables.none { it.isReified } ||
        (getter != null || setter != null) &&
        (getter == null || KModifier.INLINE in getter.modifiers) &&
        (setter == null || KModifier.INLINE in setter.modifiers)) {
      "only type parameters of properties with inline getters and/or setters can be reified!"
    }
  }

  internal fun emit(
    codeWriter: CodeWriter,
    implicitModifiers: Set<KModifier>,
    withInitializer: Boolean = true,
    emitKdoc: Boolean = true,
    inline: Boolean = false,
    inlineAnnotations: Boolean = inline
  ) {
    val isInlineProperty = getter?.modifiers?.contains(KModifier.INLINE) ?: false &&
        setter?.modifiers?.contains(KModifier.INLINE) ?: false
    val propertyModifiers = if (isInlineProperty) modifiers + KModifier.INLINE else modifiers
    if (emitKdoc) {
      codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    }
    codeWriter.emitAnnotations(annotations, inlineAnnotations)
    codeWriter.emitModifiers(propertyModifiers, implicitModifiers)
    codeWriter.emitCode(if (mutable) "var·" else "val·")
    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }
    if (receiverType != null) {
      if (receiverType is LambdaTypeName) {
        codeWriter.emitCode("(%T).", receiverType)
      } else {
        codeWriter.emitCode("%T.", receiverType)
      }
    }
    codeWriter.emitCode("%N: %T", this, type)
    if (withInitializer && initializer != null) {
      if (delegated) {
        codeWriter.emit(" by ")
      } else {
        codeWriter.emitCode(" = ")
      }
      val initializerFormat = if (initializer.hasStatements()) "%L" else "«%L»"
      codeWriter.emitCode(
          codeBlock = CodeBlock.of(initializerFormat, initializer),
          isConstantContext = KModifier.CONST in modifiers
      )
    }
    codeWriter.emitWhereBlock(typeVariables)
    if (!inline) codeWriter.emit("\n")
    val implicitAccessorModifiers = if (isInlineProperty) {
      implicitModifiers + KModifier.INLINE
    } else {
      implicitModifiers
    }
    if (getter != null) {
      codeWriter.emitCode("⇥")
      getter.emit(codeWriter, null, implicitAccessorModifiers, false)
      codeWriter.emitCode("⇤")
    }
    if (setter != null) {
      codeWriter.emitCode("⇥")
      setter.emit(codeWriter, null, implicitAccessorModifiers, false)
      codeWriter.emitCode("⇤")
    }
  }

  internal fun fromPrimaryConstructorParameter(parameter: ParameterSpec): PropertySpec {
    val builder = toBuilder()
        .addAnnotations(parameter.annotations)
    builder.isPrimaryConstructorParameter = true
    builder.modifiers += parameter.modifiers
    if (builder.kdoc.isEmpty()) {
      builder.addKdoc(parameter.kdoc)
    }
    return builder.build()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString { emit(this, emptySet()) }

  @JvmOverloads
  fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type)
    builder.mutable = mutable
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.initializer = initializer
    builder.delegated = delegated
    builder.setter = setter
    builder.getter = getter
    builder.receiverType = receiverType
    builder.tags += tagMap.tags
    builder.originatingElements += originatingElements
    return builder
  }

  class Builder internal constructor(
    internal val name: String,
    internal val type: TypeName
  ) : Taggable.Builder<PropertySpec.Builder>, OriginatingElementsHolder.Builder<PropertySpec.Builder> {
    internal var isPrimaryConstructorParameter = false
    internal var mutable = false
    internal val kdoc = CodeBlock.builder()
    internal var initializer: CodeBlock? = null
    internal var delegated = false
    internal var getter: FunSpec? = null
    internal var setter: FunSpec? = null
    internal var receiverType: TypeName? = null

    val annotations = mutableListOf<AnnotationSpec>()
    val modifiers = mutableListOf<KModifier>()
    val typeVariables = mutableListOf<TypeVariableName>()
    override val tags = mutableMapOf<KClass<*>, Any>()
    override val originatingElements = mutableListOf<Element>()

    /** True to create a `var` instead of a `val`. */
    fun mutable(mutable: Boolean = true) = apply {
      this.mutable = mutable
    }

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

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    fun initializer(format: String, vararg args: Any?) = initializer(CodeBlock.of(format, *args))

    fun initializer(codeBlock: CodeBlock) = apply {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
    }

    fun delegate(format: String, vararg args: Any?): Builder = delegate(CodeBlock.of(format, *args))

    fun delegate(codeBlock: CodeBlock) = apply {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
      this.delegated = true
    }

    fun getter(getter: FunSpec) = apply {
      require(getter.name == GETTER) { "${getter.name} is not a getter" }
      check(this.getter == null) { "getter was already set" }
      this.getter = getter
    }

    fun setter(setter: FunSpec) = apply {
      require(setter.name == SETTER) { "${setter.name} is not a setter" }
      check(this.setter == null) { "setter was already set" }
      this.setter = setter
    }

    fun receiver(receiverType: TypeName) = apply {
      this.receiverType = receiverType
    }

    fun receiver(receiverType: Type) = receiver(receiverType.asTypeName())

    fun receiver(receiverType: KClass<*>) = receiver(receiverType.asTypeName())

    fun build(): PropertySpec {
      if (KModifier.INLINE in modifiers) {
        throw IllegalArgumentException("KotlinPoet doesn't allow setting the inline modifier on " +
            "properties. You should mark either the getter, the setter, or both inline.")
      }
      for (it in modifiers) {
          if (!isPrimaryConstructorParameter) it.checkTarget(PROPERTY)
      }
      return PropertySpec(this)
    }
  }

  companion object {
    @JvmStatic fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      return Builder(name, type)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, vararg modifiers: KModifier) =
        builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier) =
        builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun builder(name: String, type: TypeName, modifiers: Iterable<KModifier>): Builder {
      return Builder(name, type)
          .addModifiers(modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, modifiers: Iterable<KModifier>) =
        builder(name, type.asTypeName(), modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, modifiers: Iterable<KModifier>) =
        builder(name, type.asTypeName(), modifiers)
  }
}
