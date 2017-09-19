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
  val members = builder.members.toImmutableMap()
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
    var hasMultipleMembers = members.size > 1
    members.entries.forEachIndexed { index, (_, member) ->
      if (index == 0) {
        hasMultipleMembers = hasMultipleMembers || member.args.size > 1
        if (hasMultipleMembers) codeWriter.emit(whitespace).indent(2)
      } else {
        codeWriter.emit(memberSeparator)
      }
      codeWriter.emitAnnotationMember(whitespace, memberSeparator, member)
    }
    if (hasMultipleMembers) codeWriter.unindent(2).emit(whitespace)
    codeWriter.emit(")")
  }

  private fun CodeWriter.emitAnnotationMember(
      whitespace: String,
      memberSeparator: String,
      member: Member) {
    if (member.emitName) {
      emitCode("%L = ", member.name)
      if (member.args.size > 1) {
        emit("[" + whitespace)
        indent(2)
      }
    }
    emitCode(member.args.joinToCode(separator = memberSeparator))
    if (member.args.size > 1 && member.emitName) {
      unindent(2)
      emit(whitespace + "]")
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(type)
    for ((key, value) in members) {
      builder.members[key] = value
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

  class Member(
      val name: String,
      internal val args: MutableList<CodeBlock> = mutableListOf(),
      val emitName: Boolean
  ) {
    override fun toString() = "$name = $args, should emit name = $emitName"
  }

  class Builder internal constructor(internal val type: TypeName) {
    internal val members = mutableMapOf<String, Member>()
    internal var useSiteTarget: UseSiteTarget? = null

    fun addMember(name: String, format: String, vararg args: Any, emitName: Boolean = false) =
        addMember(name, CodeBlock.of(format, *args), emitName)

    fun addMember(name: String, codeBlock: CodeBlock, emitName: Boolean = false) = apply {
      members.getOrPut(name, { Member(name, emitName = emitName) }).args.add(codeBlock)
    }

    /**
     * Delegates to [.addMember], with parameter `format` depending on the given `value` object.
     * Falls back to `"%L"` literal format if the class of the given `value` object is not
     * supported.
     */
    internal fun addMemberForValue(memberName: String, value: Any) = apply {
      val emitName = memberName != "value"
      when (value) {
        is Class<*> -> addMember(memberName, "%T::class", value, emitName = emitName)
        is Enum<*> -> addMember(memberName, "%T.%L", value.javaClass, value.name,
            emitName = emitName)
        is String -> addMember(memberName, "%S", value, emitName = emitName)
        is Float -> addMember(memberName, "%Lf", value, emitName = emitName)
        is Char -> addMember(memberName, "'%L'", characterLiteralWithoutSingleQuotes(value),
            emitName = emitName)
        else -> addMember(memberName, "%L", value, emitName = emitName)
      }
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
        = builder.addMember(name, "%L", get(a), emitName = name != "value")

    override fun visitEnumConstant(c: VariableElement, name: String)
        = builder.addMember(name, "%T.%L", c.asType(), c.simpleName, emitName = name != "value")

    override fun visitType(t: TypeMirror, name: String)
        = builder.addMember(name, "%T::class", t, emitName = name != "value")

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
            builder.addMember(method.name, "%L", get(value), emitName = method.name != "value")
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

    @JvmStatic fun builder(type: ClassName) = Builder(type)

    @JvmStatic fun builder(type: Class<*>) = builder(type.asClassName())

    @JvmStatic fun builder(type: KClass<*>) = builder(type.asClassName())
  }
}
