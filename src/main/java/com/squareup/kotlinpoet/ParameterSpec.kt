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
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

/** A generated parameter declaration.  */
class ParameterSpec private constructor(builder: ParameterSpec.Builder) {
  val name: String = builder.name
  val annotations: List<AnnotationSpec> = Util.immutableList(builder.annotations)
  val modifiers: Set<Modifier> = Util.immutableSet(builder.modifiers)
  val type: TypeName = builder.type

  fun hasModifier(modifier: Modifier): Boolean {
    return modifiers.contains(modifier)
  }

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, varargs: Boolean) {
    codeWriter.emitAnnotations(annotations, true)
    codeWriter.emitModifiers(modifiers)
    if (varargs) {
      codeWriter.emit("vararg %L: %T", name, TypeName.arrayComponent(type))
    } else {
      codeWriter.emit("%L: %T", name, type)
    }
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) return true
    if (o == null) return false
    if (javaClass != o.javaClass) return false
    return toString() == o.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String {
    val out = StringWriter()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter, false)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }

  }

  fun toBuilder(type: TypeName = this.type, name: String = this.name): Builder {
    val builder = Builder(type, name)
    builder.annotations.addAll(annotations)
    builder.modifiers.addAll(modifiers)
    return builder
  }

  class Builder internal constructor(
      internal val type: TypeName,
      internal val name: String) {
    internal val annotations = ArrayList<AnnotationSpec>()
    internal val modifiers = ArrayList<Modifier>()

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

    fun addModifiers(modifiers: Iterable<Modifier>): Builder {
      for (modifier in modifiers) {
        this.modifiers.add(modifier)
      }
      return this
    }

    fun build() = ParameterSpec(this)
  }

  companion object {
    @JvmStatic fun get(element: VariableElement): ParameterSpec {
      val type = TypeName.get(element.asType())
      val name = element.simpleName.toString()
      return ParameterSpec.builder(type, name)
          .addModifiers(element.modifiers)
          .build()
    }

    @JvmStatic fun parametersOf(method: ExecutableElement)
        = method.parameters.map { ParameterSpec.get(it) }

    @JvmStatic fun builder(type: TypeName, name: String, vararg modifiers: Modifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(type, name).addModifiers(*modifiers)
    }

    @JvmStatic fun builder(type: Type, name: String, vararg modifiers: Modifier)
        = builder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun builder(type: KClass<*>, name: String, vararg modifiers: Modifier)
        = builder(TypeName.get(type), name, *modifiers)
  }
}
