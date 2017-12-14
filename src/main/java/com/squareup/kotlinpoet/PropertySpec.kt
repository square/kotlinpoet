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
import java.lang.reflect.Type
import kotlin.reflect.KClass

/** A generated property declaration.  */
class PropertySpec private constructor(builder: Builder) {
  val mutable = builder.mutable
  val name = builder.name
  val type = builder.type
  val kdoc = builder.kdoc.build()
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val initializer = builder.initializer
  val delegated = builder.delegated
  val getter = builder.getter
  val setter = builder.setter
  val receiverType = builder.receiverType

  internal fun emit(
    codeWriter: CodeWriter,
    implicitModifiers: Set<KModifier>,
    withInitializer: Boolean = true,
    inline: Boolean = false
  ) {
    val isInlineProperty = getter?.modifiers?.contains(KModifier.INLINE) ?: false &&
        setter?.modifiers?.contains(KModifier.INLINE) ?: false
    val propertyModifiers = if (isInlineProperty) modifiers + KModifier.INLINE else modifiers
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(propertyModifiers, implicitModifiers)
    codeWriter.emit(if (mutable) "var " else "val ")
    if (receiverType != null) {
      if (receiverType is LambdaTypeName) {
        codeWriter.emitCode("(%T).", receiverType)
      } else {
        codeWriter.emitCode("%T.", receiverType)
      }
    }
    codeWriter.emitCode("%L: %T", name, type)
    if (withInitializer && initializer != null) {
      if (delegated) {
        codeWriter.emit(" by ")
      } else {
        codeWriter.emitCode(" =%W")
      }
      codeWriter.emitCode("%[%L%]", initializer)
    }
    if (!inline) codeWriter.emit("\n")
    val implicitAccessorModifiers = if (isInlineProperty) {
      implicitModifiers + KModifier.INLINE
    } else {
      implicitModifiers
    }
    if (getter != null) {
      codeWriter.emitCode("%>")
      getter.emit(codeWriter, null, implicitAccessorModifiers)
      codeWriter.emitCode("%<")
    }
    if (setter != null) {
      codeWriter.emitCode("%>")
      setter.emit(codeWriter, null, implicitAccessorModifiers)
      codeWriter.emitCode("%<")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString { emit(CodeWriter(this), emptySet()) }

  fun toBuilder(): Builder {
    val builder = Builder(name, type)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.initializer = initializer
    builder.delegated = delegated
    return builder
  }

  class Builder internal constructor(internal val name: String, internal val type: TypeName) {
    internal var mutable = false
    internal val kdoc = CodeBlock.builder()
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()
    internal var initializer: CodeBlock? = null
    internal var delegated = false
    internal var getter: FunSpec? = null
    internal var setter: FunSpec? = null
    internal var receiverType: TypeName? = null

    fun mutable(mutable: Boolean) = apply {
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
      for (modifier in modifiers) {
        modifier.checkTarget(KModifier.Target.PROPERTY)
      }
      this.modifiers += modifiers
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

    fun build() = PropertySpec(this)
  }

  companion object {
    @JvmStatic fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, vararg modifiers: KModifier)
        = builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = builder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun varBuilder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      require(name.isName) { "not a valid name: $name" }
      return Builder(name, type)
          .mutable(true)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun varBuilder(name: String, type: Type, vararg modifiers: KModifier)
        = varBuilder(name, type.asTypeName(), *modifiers)

    @JvmStatic fun varBuilder(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = varBuilder(name, type.asTypeName(), *modifiers)
  }
}
