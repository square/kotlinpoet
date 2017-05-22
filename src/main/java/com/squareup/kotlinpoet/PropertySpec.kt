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
import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass

/** A generated property declaration.  */
class PropertySpec private constructor(builder: Builder) {
  val mutable: Boolean = builder.mutable
  val name: String = builder.name
  val type: TypeName = builder.type
  val kdoc: CodeBlock = builder.kdoc.build()
  val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  val initializer: CodeBlock? = builder.initializer
  val delegated: Boolean = builder.delegated
  val getter: FunSpec? = builder.getter
  val setter: FunSpec? = builder.setter

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, implicitModifiers: Set<KModifier>) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)
    codeWriter.emit(if (mutable) "var " else "val ")
    codeWriter.emitCode("%L: %T", name, type)
    if (initializer != null) {
      if (delegated) {
        codeWriter.emit(" by ")
      } else {
        codeWriter.emit(" = ")
      }
      codeWriter.emitCode("%[%L%]", initializer)
    }
    codeWriter.emit("\n")
    if (getter != null) {
      codeWriter.emitCode("%>")
      getter.emit(codeWriter, null, implicitModifiers)
      codeWriter.emitCode("%<")
    }
    if (setter != null) {
      codeWriter.emitCode("%>")
      setter.emit(codeWriter, null, implicitModifiers)
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

  override fun toString(): String {
    val out = StringWriter()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter, emptySet<KModifier>())
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

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
    internal var getter : FunSpec? = null
    internal var setter : FunSpec? = null

    fun mutable(mutable: Boolean): Builder {
      this.mutable = mutable
      return this
    }

    fun addKdoc(format: String, vararg args: Any): Builder {
      kdoc.add(format, *args)
      return this
    }

    fun addKdoc(block: CodeBlock): Builder {
      kdoc.add(block)
      return this
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder {
      annotations += annotationSpecs
      return this
    }

    fun addAnnotation(annotationSpec: AnnotationSpec): Builder {
      annotations += annotationSpec
      return this
    }

    fun addAnnotation(annotation: ClassName): Builder {
      annotations += AnnotationSpec.builder(annotation).build()
      return this
    }

    fun addAnnotation(annotation: Class<*>) = addAnnotation(ClassName.get(annotation))

    fun addModifiers(vararg modifiers: KModifier): Builder {
      for (modifier in modifiers) {
        modifier.checkTarget(KModifier.Target.PROPERTY)
      }
      this.modifiers += modifiers
      return this
    }

    fun initializer(format: String, vararg args: Any?) = initializer(CodeBlock.of(format, *args))

    fun initializer(codeBlock: CodeBlock): Builder {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
      return this
    }

    fun delegate(format: String, vararg args: Any?): Builder = delegate(CodeBlock.of(format, *args))

    fun delegate(codeBlock: CodeBlock): Builder {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
      this.delegated = true
      return this
    }

    fun getter(getter: FunSpec): Builder {
      require(getter.name == GETTER) { "${getter.name} is not a getter" }
      check(this.getter == null ) { "getter was already set" }
      this.getter = getter
      return this
    }

    fun setter(setter: FunSpec): Builder {
      require(setter.name == SETTER) { "${setter.name} is not a setter" }
      check(this.setter == null ) { "setter was already set" }
      this.setter = setter
      return this
    }

    fun build() = PropertySpec(this)
  }

  companion object {
    @JvmStatic fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(name, type)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun builder(name: String, type: Type, vararg modifiers: KModifier)
        = builder(name, TypeName.get(type), *modifiers)

    @JvmStatic fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = builder(name, TypeName.get(type), *modifiers)

    @JvmStatic fun varBuilder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(name, type)
          .mutable(true)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun varBuilder(name: String, type: Type, vararg modifiers: KModifier)
        = varBuilder(name, TypeName.get(type), *modifiers)

    @JvmStatic fun varBuilder(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = varBuilder(name, TypeName.get(type), *modifiers)
  }
}
