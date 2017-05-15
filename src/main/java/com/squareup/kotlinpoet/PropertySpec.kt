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

import java.io.IOException
import java.io.StringWriter
import java.lang.reflect.Type
import javax.lang.model.SourceVersion
import kotlin.reflect.KClass

/** A generated property declaration.  */
class PropertySpec constructor(
    val mutable: Boolean,
    val type: TypeName,
    val name: String,
    val kdoc: CodeBlock,
    annotations: Collection<AnnotationSpec>,
    modifiers: Collection<KModifier>,
    val initializer: CodeBlock?) {
  val annotations = annotations.toList()
  val modifiers = modifiers.toSet()

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, implicitModifiers: Set<KModifier>) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)
    codeWriter.emit(if (mutable) "var " else "val ")
    codeWriter.emit("%L: %T", name, type)
    if (initializer != null) {
      codeWriter.emit(" = ")
      codeWriter.emit(initializer)
    }
    codeWriter.emit("\n")
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
    val builder = Builder(type, name)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.initializer = initializer
    return builder
  }

  class Builder internal constructor(internal val type: TypeName, internal val name: String) {
    internal var mutable = false
    internal val kdoc = CodeBlock.builder()
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()
    internal var initializer: CodeBlock? = null

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

    fun build() = PropertySpec(
        mutable, type, name, kdoc.build(), annotations, modifiers, initializer)
  }

  companion object {
    @JvmStatic fun builder(type: TypeName, name: String, vararg modifiers: KModifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(type, name)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun builder(type: Type, name: String, vararg modifiers: KModifier)
        = builder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun builder(type: KClass<*>, name: String, vararg modifiers: KModifier)
        = builder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun varBuilder(type: TypeName, name: String, vararg modifiers: KModifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(type, name)
          .mutable(true)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun varBuilder(type: Type, name: String, vararg modifiers: KModifier)
        = varBuilder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun varBuilder(type: KClass<*>, name: String, vararg modifiers: KModifier)
        = varBuilder(TypeName.get(type), name, *modifiers)
  }
}
