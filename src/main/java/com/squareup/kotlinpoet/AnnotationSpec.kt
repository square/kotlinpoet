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

import com.squareup.kotlinpoet.ClassName.Companion.asClassName
import com.squareup.kotlinpoet.TypeName.Companion.asTypeName
import java.io.IOException
import java.io.StringWriter
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
  val members: Map<String, List<CodeBlock>> = builder.members.toImmutableMultimap()

  @Throws(IOException::class)
  internal fun emit(codeWriter: CodeWriter, inline: Boolean) {
    val whitespace = if (inline) "" else "\n"
    val memberSeparator = if (inline) ", " else ",\n"
    if (members.isEmpty()) {
      // @Singleton
      codeWriter.emitCode("@%T", type)
    } else if (members.size == 1 && members.containsKey("value")) {
      // @Named("foo")
      codeWriter.emitCode("@%T(", type)
      emitAnnotationValues(codeWriter, whitespace, memberSeparator, members["value"]!!)
      codeWriter.emit(")")
    } else {
      // Inline:
      //   @Column(name = "updated_at", nullable = false)
      //
      // Not inline:
      //   @Column(
      //       name = "updated_at",
      //       nullable = false
      //   )
      codeWriter.emitCode("@%T(" + whitespace, type)
      codeWriter.indent(2)
      val i = members.entries.iterator()
      while (i.hasNext()) {
        val entry = i.next()
        codeWriter.emitCode("%L = ", entry.key)
        emitAnnotationValues(codeWriter, whitespace, memberSeparator, entry.value)
        if (i.hasNext()) codeWriter.emit(memberSeparator)
      }
      codeWriter.unindent(2)
      codeWriter.emit(whitespace + ")")
    }
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

    codeWriter.emitCode(values[0])
    codeWriter.emit(whitespace)
    if (values.size > 1) {
      codeWriter.indent(2)
      for (i in 1 until values.size) {
        if (i > 1) codeWriter.emit(memberSeparator)
        codeWriter.emitCode(values[i])
      }
      codeWriter.unindent(2)
      codeWriter.emit(whitespace + ")")
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(type)
    for ((key, value) in members) {
      builder.members.put(key, value.toMutableList())
    }
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
    val out = StringWriter()
    try {
      val codeWriter = CodeWriter(out)
      codeWriter.emitCode("%L", this)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }

  }

  class Builder internal constructor(internal val type: TypeName) {
    internal val members = mutableMapOf<String, MutableList<CodeBlock>>()

    fun addMember(name: String, format: String, vararg args: Any) =
        addMember(name, null, CodeBlock.of(format, *args))

    fun addMemberArrayElement(name: String, arrayType: KClass<*>, format: String,
                              vararg args: Any) =
        addMember(name, arrayType.asTypeName(), CodeBlock.of(format, *args))

    fun addMemberArrayElement(name: String, arrayType: TypeName, format: String, vararg args: Any) =
        addMember(name, arrayType, CodeBlock.of(format, *args))

    private fun addMember(name: String, arrayType: TypeName?, format: String, vararg args: Any) =
        addMember(name, arrayType, CodeBlock.of(format, *args))

    private fun addMember(name: String, arrayType: TypeName?, codeBlock: CodeBlock): Builder {
      var codeBlocks = members[name]
      if (codeBlocks == null) {
        codeBlocks = mutableListOf()
        if (arrayType != null) {
          codeBlocks.add(arrayType.toCreateCodeBlock())
        }
        members[name] = codeBlocks
      }
      codeBlocks.add(codeBlock)
      return this
    }

    private fun TypeName.toCreateCodeBlock() = when (this) {
      BooleanArray::class.asTypeName() -> CodeBlock.of("booleanArrayOf(")
      ByteArray::class.asTypeName() -> CodeBlock.of("byteArrayOf(")
      CharArray::class.asTypeName() -> CodeBlock.of("charArrayOf(")
      DoubleArray::class.asTypeName() -> CodeBlock.of("doubleArrayOf(")
      FloatArray::class.asTypeName() -> CodeBlock.of("floatArrayOf(")
      IntArray::class.asTypeName() -> CodeBlock.of("intArrayOf(")
      LongArray::class.asTypeName() -> CodeBlock.of("longArrayOf(")
      ShortArray::class.asTypeName() -> CodeBlock.of("shortArrayOf(")
      else -> CodeBlock.of("arrayOf(")
    }

    /**
     * Delegates to [.addMember], with parameter `format` depending on the given `value` object.
     * Falls back to `"%L"` literal format if the class of the given `value` object is not
     * supported.
     */
    internal fun addMemberForValue(memberName: String, value: Any, arrayType: TypeName? = null) =
        when (value) {
          is Class<*> -> addMember(memberName, arrayType, "%T::class", value)
          is Enum<*> -> addMember(memberName, arrayType, "%T.%L", value.javaClass, value.name)
          is String -> addMember(memberName, arrayType, "%S", value)
          is Float -> addMember(memberName, arrayType, "%Lf", value)
          is Char -> addMember(memberName, arrayType, "'%L'",
              characterLiteralWithoutSingleQuotes(value))
          else -> addMember(memberName, arrayType, "%L", value)
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
      var arrayElementsVisitor: ArrayElementsVisitor? = null
      for (value in values) {
        if (arrayElementsVisitor == null) {
          arrayElementsVisitor = ArrayElementsVisitor(builder, value.value.toArrayTypeName())
        }
        value.accept(arrayElementsVisitor, name)
      }
      return builder
    }

    private fun Any.toArrayTypeName() = when (this) {
      is Boolean -> BooleanArray::class.asTypeName()
      is Byte -> ByteArray::class.asTypeName()
      is Char -> CharArray::class.asTypeName()
      is Double -> DoubleArray::class.asTypeName()
      is Float -> FloatArray::class.asTypeName()
      is Int -> IntArray::class.asTypeName()
      is Long -> LongArray::class.asTypeName()
      is Short -> ShortArray::class.asTypeName()
      else -> ParameterizedTypeName.get(ARRAY, ANY)
    }
  }

  private class ArrayElementsVisitor internal constructor(
      internal val builder: Builder,
      private val arrayType: TypeName)
    : SimpleAnnotationValueVisitor7<Builder, String>(builder) {

    override fun defaultAction(o: Any, name: String)
        = builder.addMemberForValue(name, o, arrayType)

    override fun visitAnnotation(a: AnnotationMirror, name: String)
        = builder.addMemberArrayElement(name, arrayType, "%L", get(a))

    override fun visitEnumConstant(c: VariableElement, name: String)
        = builder.addMemberArrayElement(name, arrayType, "%T.%L", c.asType(),
        c.simpleName)

    override fun visitType(t: TypeMirror, name: String)
        = builder.addMemberArrayElement(name, arrayType, "%T::class", t)

    override fun visitArray(values: List<AnnotationValue>, name: String): Builder {
      // never happens, since multidimensional arrays are not supported
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
              builder.addMemberForValue(method.name, Array.get(value, i), value::class.asTypeName())
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
