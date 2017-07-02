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
import java.lang.reflect.Array
import java.util.Arrays
import java.util.Objects
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor7
import kotlin.reflect.KClass

/** A generated annotation on a declaration.  */
class AnnotationSpec private constructor(builder: AnnotationSpec.Builder) {
  val type: TypeName = builder.type
  val members = builder.members.toImmutableMultimap()
  val useSiteTarget: UseSiteTarget? = builder.useSiteTarget

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, inline: Boolean, asParameter: Boolean = false) {
    if (!asParameter) {
      codeWriter.emit("@")
    }
    if (useSiteTarget != null) {
      codeWriter.emit(useSiteTarget.keyword + ":")
    }
    codeWriter.emitCode("%T", type)

    if (members.isEmpty() && !asParameter) {
      // @Singleton
      return
    }

    val whitespace = if (inline) "" else "\n"
    val memberSeparator = if (inline) ", " else ",\n"

    codeWriter.emit("(")
    if (members.size == 1 && members.containsKey("value")) {
      // @Named("foo")
      emitAnnotationValues(codeWriter, whitespace, memberSeparator, members["value"]!!)
    } else {
      // Inline:
      //   @Column(name = "updated_at", nullable = false)
      //
      // Not inline:
      //   @Column(
      //       name = "updated_at",
      //       nullable = false
      //   )
      codeWriter.emit(whitespace).indent(2)
      members.entries.forEachIndexed { index, entry ->
        if (index > 0) codeWriter.emit(memberSeparator)
        codeWriter.emitCode("%L = ", entry.key)
        emitAnnotationValues(codeWriter, whitespace, memberSeparator, entry.value)
      }
      codeWriter.unindent(2).emit(whitespace)
    }
    codeWriter.emit(")")
  }

  @Throws(IOException::class)
  private fun emitAnnotationValues(
      codeWriter: CodeWriter,
      whitespace: String,
      memberSeparator: String,
      values: List<CodeBlock>) {
    if (values.size == 1) {
      codeWriter.indent(2)
      codeWriter.emitCode(values[0])
      codeWriter.unindent(2)
      return
    }

    codeWriter.emit("[" + whitespace)
    codeWriter.indent(2)
    values.forEachIndexed { index, value ->
      if (index > 0) codeWriter.emit(memberSeparator)
      codeWriter.emitCode(value)
    }
    codeWriter.unindent(2)
    codeWriter.emit(whitespace + "]")
  }

  fun toBuilder(): Builder {
    val builder = Builder(type)
    for ((key, value) in members) {
      builder.members.put(key, value.toMutableList())
    }
    builder.useSiteTarget = useSiteTarget
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int {
    return toString().hashCode()
  }

  override fun toString(): String {
    val out = StringBuilder()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter, inline = true, asParameter = false)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  enum class UseSiteTarget(internal val keyword: String) {
    FILE("file"),
    PROPERTY("property"),
    FIELD("field"),
    GET("get"),
    SET("set"),
    RECEIVER("receiver"),
    PARAM("param"),
    SETPARAM("setparam"),
    DELEGATE("delegate")
  }

  class Builder internal constructor(internal val type: TypeName) {
    internal val members = mutableMapOf<String, MutableList<CodeBlock>>()
    internal var useSiteTarget: UseSiteTarget? = null

    fun addMember(name: String, format: String, vararg args: Any) =
        addMember(name, CodeBlock.of(format, *args))

    fun addMember(name: String, codeBlock: CodeBlock) = apply {
      members.getOrPut(name, { mutableListOf() }).add(codeBlock)
    }

    /**
     * Delegates to [.addMember], with parameter `format` depending on the given `value` object.
     * Falls back to `"%L"` literal format if the class of the given `value` object is not
     * supported.
     */
    internal fun addMemberForValue(memberName: String, value: Any) = when (value) {
      is Class<*> -> addMember(memberName, "%T::class", value)
      is Enum<*> -> addMember(memberName, "%T.%L", value.javaClass, value.name)
      is String -> addMember(memberName, "%S", value)
      is Float -> addMember(memberName, "%Lf", value)
      is Char -> addMember(memberName, "'%L'", characterLiteralWithoutSingleQuotes(value))
      else -> addMember(memberName, "%L", value)
    }

    fun useSiteTarget(useSiteTarget: UseSiteTarget?) = apply {
      this.useSiteTarget = useSiteTarget
    }

    fun build() = AnnotationSpec(this)
  }

  /**
   * Annotation value visitor adding members to the given builder instance.
   */
  private class Visitor internal constructor(internal val builder: Builder)
    : SimpleAnnotationValueVisitor7<Builder, String>(builder) {

    override fun defaultAction(o: Any, name: String)
        = builder.addMemberForValue(name, o)

    override fun visitAnnotation(a: AnnotationMirror, name: String)
        = builder.addMember(name, "%L", get(a))

    override fun visitEnumConstant(c: VariableElement, name: String)
        = builder.addMember(name, "%T.%L", c.asType(), c.simpleName)

    override fun visitType(t: TypeMirror, name: String)
        = builder.addMember(name, "%T::class", t)

    override fun visitArray(values: List<AnnotationValue>, name: String): Builder {
      for (value in values) {
        value.accept(this, name)
      }
      return builder
    }
  }

  companion object {
    @JvmStatic @JvmOverloads fun get(
        annotation: Annotation,
        includeDefaultValues: Boolean = false): AnnotationSpec {
      try {
        val javaAnnotation = annotation as java.lang.annotation.Annotation
        val builder = builder(javaAnnotation.annotationType())
        val methods = annotation.annotationType().declaredMethods
        Arrays.sort(methods, { m1, m2 -> m1.name.compareTo(m2.name) })
        for (method in methods) {
          val value = method.invoke(annotation)
          if (!includeDefaultValues) {
            if (Objects.deepEquals(value, method.defaultValue)) {
              continue
            }
          }
          if (value.javaClass.isArray) {
            for (i in 0 until Array.getLength(value)) {
              builder.addMemberForValue(method.name, Array.get(value, i))
            }
            continue
          }
          if (value is Annotation) {
            builder.addMember(method.name, "%L", get(value))
            continue
          }
          builder.addMemberForValue(method.name, value)
        }
        return builder.build()
      } catch (e: Exception) {
        throw RuntimeException("Reflecting $annotation failed!", e)
      }
    }

    @JvmStatic fun get(annotation: AnnotationMirror): AnnotationSpec {
      val element = annotation.annotationType.asElement() as TypeElement
      val builder = AnnotationSpec.builder(element.asClassName())
      val visitor = Visitor(builder)
      for (executableElement in annotation.elementValues.keys) {
        val name = executableElement.simpleName.toString()
        val value = annotation.elementValues[executableElement]!!
        value.accept(visitor, name)
      }
      return builder.build()
    }

    @JvmStatic fun builder(type: ClassName): Builder {
      return Builder(type)
    }

    @JvmStatic fun builder(type: Class<*>): Builder {
      return builder(type.asClassName())
    }

    @JvmStatic fun builder(type: KClass<*>): Builder {
      return builder(type.asClassName())
    }
  }
}
