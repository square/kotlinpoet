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
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import kotlin.reflect.KClass

/** A generated parameter declaration.  */
class ParameterSpec private constructor(builder: ParameterSpec.Builder) {
  val name: String = builder.name
  val annotations: List<AnnotationSpec> = Util.immutableList(builder.annotations)
  val modifiers: Set<KModifier> = Util.immutableSet(builder.modifiers)
  val type: TypeName = builder.type

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, varargs: Boolean) {
    codeWriter.emitAnnotations(annotations, true)
    codeWriter.emitJavaModifiers(modifiers)
    if (varargs) {
      codeWriter.emit("vararg %L: %T", name, TypeName.arrayComponent(type))
    } else {
      codeWriter.emit("%L: %T", name, type)
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
      emit(codeWriter, false)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }

  }

  fun toBuilder(type: TypeName = this.type, name: String = this.name): Builder {
    val builder = Builder(type, name)
    builder.annotations += annotations
    builder.modifiers += modifiers
    return builder
  }

  class Builder internal constructor(
      internal val type: TypeName,
      internal val name: String) {
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()

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
      this.modifiers += modifiers
      return this
    }

    fun addModifiers(modifiers: Iterable<KModifier>): Builder {
      this.modifiers += modifiers
      return this
    }

    fun jvmModifiers(modifiers: Iterable<Modifier>): Builder {
      for (modifier in modifiers) {
        when (modifier) {
          Modifier.FINAL -> this.modifiers += KModifier.FINAL
          else -> throw IllegalArgumentException("unexpected parameter modifier $modifier")
        }
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
          .jvmModifiers(element.modifiers)
          .build()
    }

    @JvmStatic fun parametersOf(method: ExecutableElement)
        = method.parameters.map { ParameterSpec.get(it) }

    @JvmStatic fun builder(type: TypeName, name: String, vararg modifiers: KModifier): Builder {
      require(SourceVersion.isName(name)) { "not a valid name: $name" }
      return Builder(type, name).addModifiers(*modifiers)
    }

    @JvmStatic fun builder(type: Type, name: String, vararg modifiers: KModifier)
        = builder(TypeName.get(type), name, *modifiers)

    @JvmStatic fun builder(type: KClass<*>, name: String, vararg modifiers: KModifier)
        = builder(TypeName.get(type), name, *modifiers)
  }
}
