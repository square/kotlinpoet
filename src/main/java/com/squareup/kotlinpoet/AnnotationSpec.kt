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

import kotlin.reflect.KClass

/** A generated annotation on a declaration.  */
class AnnotationSpec private constructor(builder: AnnotationSpec.Builder) {
  val type: TypeName = builder.type
  val members = builder.members.toImmutableMultimap()
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
    codeWriter.emitCode(values.joinToCode(separator = memberSeparator))
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

  companion object {
    @JvmStatic fun builder(type: ClassName): Builder {
      return Builder(type)
    }

    @JvmStatic fun builder(type: KClass<*>): Builder {
      return builder(type.asClassName())
    }
  }
}
