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
import java.util.ArrayList
import java.util.Collections
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

/** A generated property declaration.  */
class PropertySpec private constructor(builder: PropertySpec.Builder) {
  val type: TypeName = builder.type
  val name: String = builder.name
  val kdoc: CodeBlock = builder.kdoc.build()
  val annotations: List<AnnotationSpec> = Util.immutableList(builder.annotations)
  val modifiers: Set<Modifier> = Util.immutableSet(builder.modifiers)
  val initializer: CodeBlock = builder.initializer ?: CodeBlock.builder().build()

  fun hasModifier(modifier: Modifier) = modifiers.contains(modifier)

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, implicitModifiers: Set<Modifier>) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)
    codeWriter.emit("%L: %T", name, type)
    if (!initializer.isEmpty()) {
      codeWriter.emit(" = ")
      codeWriter.emit(initializer)
    }
    codeWriter.emit(";\n")
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
      emit(codeWriter, emptySet<Modifier>())
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(type, name)
    builder.kdoc.add(kdoc)
    builder.annotations.addAll(annotations)
    builder.modifiers.addAll(modifiers)
    builder.initializer = if (initializer.isEmpty()) null else initializer
    return builder
  }

  class Builder internal constructor(internal val type: TypeName, internal val name: String) {
    internal val kdoc = CodeBlock.builder()
    internal val annotations = ArrayList<AnnotationSpec>()
    internal val modifiers = ArrayList<Modifier>()
    internal var initializer: CodeBlock? = null

    fun addKdoc(format: String, vararg args: Any): Builder {
      kdoc.add(format, *args)
      return this
    }

    fun addKdoc(block: CodeBlock): Builder {
      kdoc.add(block)
      return this
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder {
      for (annotationSpec in annotationSpecs) {
        this.annotations.add(annotationSpec)
      }
      return this
    }

    fun addAnnotation(annotationSpec: AnnotationSpec): Builder {
      this.annotations.add(annotationSpec)
      return this
    }

    fun addAnnotation(annotation: ClassName): Builder {
      this.annotations.add(AnnotationSpec.builder(annotation).build())
      return this
    }

    fun addAnnotation(annotation: Class<*>) = addAnnotation(ClassName.get(annotation))

    fun addModifiers(vararg modifiers: Modifier): Builder {
      Collections.addAll(this.modifiers, *modifiers)
      return this
    }

    fun initializer(format: String, vararg args: Any?) = initializer(CodeBlock.of(format, *args))

    fun initializer(codeBlock: CodeBlock): Builder {
      check(this.initializer == null) { "initializer was already set" }
      this.initializer = codeBlock
      return this
    }

    fun build() = PropertySpec(this)
  }

  companion object {
    @JvmStatic fun builder(type: TypeName, name: String, vararg modifiers: Modifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(type, name)
          .addModifiers(*modifiers)
    }

    @JvmStatic fun builder(type: Type, name: String, vararg modifiers: Modifier)
        = builder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun builder(type: KClass<*>, name: String, vararg modifiers: Modifier)
        = builder(TypeName.get(type), name, *modifiers)
  }
}
