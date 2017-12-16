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

import java.lang.reflect.Array
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
  val members = builder.members.toImmutableList()
  val useSiteTarget: UseSiteTarget? = builder.useSiteTarget

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

    // Inline:
    //   @Column(name = "updated_at", nullable = false)
    //
    // Not inline:
    //   @Column(
    //       name = "updated_at",
    //       nullable = false
    //   )

    codeWriter.emit("(")
    if (members.size > 1) codeWriter.emit(whitespace).indent(2)
    codeWriter.emitCode(members
        .map { it.replaceAll("%W", whitespace) }
        .map { if (inline) it.replaceAll("[%>|%<]", "") else it }
        .joinToCode(separator = memberSeparator))
    if (members.size > 1) codeWriter.unindent(2).emit(whitespace)
    codeWriter.emit(")")
  }

  fun toBuilder(): Builder {
    val builder = Builder(type)
    builder.members += members
    builder.useSiteTarget = useSiteTarget
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildString {
    emit(CodeWriter(this), inline = true, asParameter = false)
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
    internal val members = mutableListOf<CodeBlock>()
    internal var useSiteTarget: UseSiteTarget? = null

    fun addMember(format: String, vararg args: Any) =
        addMember(CodeBlock.of(format, *args))

    fun addMember(codeBlock: CodeBlock) = apply {
      members += codeBlock
    }

    fun useSiteTarget(useSiteTarget: UseSiteTarget?) = apply {
      this.useSiteTarget = useSiteTarget
    }

    fun build() = AnnotationSpec(this)

    companion object {

      /**
       * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
       * Handles a number of special cases, such as appending "f" to `Float` values, and uses
       * `%L` for other types.
       */
      internal fun memberForValue(value: Any) = when (value) {
        is Class<*> -> CodeBlock.of("%T::class", value)
        is Enum<*> -> CodeBlock.of("%T.%L", value.javaClass, value.name)
        is String -> CodeBlock.of("%S", value)
        is Float -> CodeBlock.of("%Lf", value)
        is Char -> CodeBlock.of("'%L'", characterLiteralWithoutSingleQuotes(value))
        else -> CodeBlock.of("%L", value)
      }
    }
  }

  /**
   * Annotation value visitor adding members to the given builder instance.
   */
  private class Visitor internal constructor(
    internal val builder: CodeBlock.Builder
  ) : SimpleAnnotationValueVisitor7<CodeBlock.Builder, String>(builder) {

    override fun defaultAction(o: Any, name: String)
        = builder.add(Builder.memberForValue(o))

    override fun visitAnnotation(a: AnnotationMirror, name: String)
        = builder.add("%L", get(a))

    override fun visitEnumConstant(c: VariableElement, name: String)
        = builder.add("%T.%L", c.asType(), c.simpleName)

    override fun visitType(t: TypeMirror, name: String)
        = builder.add("%T::class", t)

    override fun visitArray(values: List<AnnotationValue>, name: String): CodeBlock.Builder {
      builder.add("[%W%>%>")
      values.forEachIndexed { index, value ->
        if (index > 0) builder.add(",%W")
        value.accept(this, name)
      }
      builder.add("%W%<%<]")
      return builder
    }
  }

  companion object {
    @JvmStatic @JvmOverloads fun get(
      annotation: Annotation,
      includeDefaultValues: Boolean = false
    ): AnnotationSpec {
      try {
        val javaAnnotation = annotation as java.lang.annotation.Annotation
        val builder = builder(javaAnnotation.annotationType())
        val methods = annotation.annotationType().declaredMethods.sortedBy { it.name }
        for (method in methods) {
          val value = method.invoke(annotation)
          if (!includeDefaultValues) {
            if (Objects.deepEquals(value, method.defaultValue)) {
              continue
            }
          }
          val member = CodeBlock.builder()
          if (method.name != "value") {
            member.add("%L = ", method.name)
          }
          if (value.javaClass.isArray) {
            member.add("[%W%>%>")
            for (i in 0 until Array.getLength(value)) {
              if (i > 0) member.add(",%W")
              member.add(Builder.memberForValue(Array.get(value, i)))
            }
            member.add("%W%<%<]")
            builder.addMember(member.build())
            continue
          }
          if (value is Annotation) {
            member.add("%L", get(value))
            builder.addMember(member.build())
            continue
          }
          member.add("%L", Builder.memberForValue(value))
          builder.addMember(member.build())
        }
        return builder.build()
      } catch (e: Exception) {
        throw RuntimeException("Reflecting $annotation failed!", e)
      }
    }

    @JvmStatic fun get(annotation: AnnotationMirror): AnnotationSpec {
      val element = annotation.annotationType.asElement() as TypeElement
      val builder = AnnotationSpec.builder(element.asClassName())
      for (executableElement in annotation.elementValues.keys) {
        val member = CodeBlock.builder()
        val visitor = Visitor(member)
        val name = executableElement.simpleName.toString()
        if (name != "value") {
          member.add("%L = ", name)
        }
        val value = annotation.elementValues[executableElement]!!
        value.accept(visitor, name)
        builder.addMember(member.build())
      }
      return builder.build()
    }

    @JvmStatic fun builder(type: ClassName) = Builder(type)

    @JvmStatic fun builder(type: Class<*>) = builder(type.asClassName())

    @JvmStatic fun builder(type: KClass<*>) = builder(type.asClassName())
  }
}
