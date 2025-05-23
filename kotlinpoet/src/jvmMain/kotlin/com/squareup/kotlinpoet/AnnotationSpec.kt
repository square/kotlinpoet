/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import javax.lang.model.util.SimpleAnnotationValueVisitor8
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.reflect.KClass

/** A generated annotation on a declaration. */
public class AnnotationSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
) : Taggable by tagMap {
  @Deprecated(
    message = "Use typeName instead. This property will be removed in KotlinPoet 2.0.",
    replaceWith = ReplaceWith("typeName"),
  )
  public val className: ClassName
    get() = typeName as? ClassName ?: error("ClassName is not available. Call typeName instead.")
  public val typeName: TypeName = builder.typeName
  public val members: List<CodeBlock> = builder.members.toImmutableList()
  public val useSiteTarget: UseSiteTarget? = builder.useSiteTarget

  /** Lazily-initialized toString of this AnnotationSpec.  */
  private val cachedString by lazy(NONE) {
    buildCodeString {
      emit(this, inline = true, asParameter = false)
    }
  }

  internal fun emit(codeWriter: CodeWriter, inline: Boolean, asParameter: Boolean = false) {
    if (!asParameter) {
      codeWriter.emit("@")
    }
    if (useSiteTarget != null) {
      codeWriter.emit(useSiteTarget.keyword + ":")
    }
    codeWriter.emitCode("%T", typeName)

    if (members.isEmpty() && !asParameter) {
      // @Singleton
      return
    }

    val whitespace = if (inline) "" else "\n"
    val memberSeparator = if (inline) ", " else ",\n"
    val memberSuffix = if (!inline && members.size > 1) "," else ""

    // Inline:
    //   @Column(name = "updated_at", nullable = false)
    //
    // Not inline:
    //   @Column(
    //       name = "updated_at",
    //       nullable = false,
    //   )

    codeWriter.emit("(")
    if (members.size > 1) codeWriter.emit(whitespace).indent(1)
    codeWriter.emitCode(
      codeBlock = members
        .map { if (inline) it.replaceAll("[⇥|⇤]", "") else it }
        .joinToCode(separator = memberSeparator, suffix = memberSuffix),
      isConstantContext = true,
    )
    if (members.size > 1) codeWriter.unindent(1).emit(whitespace)
    codeWriter.emit(")")
  }

  public fun toBuilder(): Builder {
    val builder = Builder(typeName)
    builder.members += members
    builder.useSiteTarget = useSiteTarget
    builder.tags += tagMap.tags
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = cachedString

  public enum class UseSiteTarget(internal val keyword: String) {
    FILE("file"),
    PROPERTY("property"),
    FIELD("field"),
    GET("get"),
    SET("set"),
    RECEIVER("receiver"),
    PARAM("param"),
    SETPARAM("setparam"),
    DELEGATE("delegate"),
  }

  public class Builder internal constructor(
    internal val typeName: TypeName,
  ) : Taggable.Builder<Builder> {
    internal var useSiteTarget: UseSiteTarget? = null

    public val members: MutableList<CodeBlock> = mutableListOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()

    public fun addMember(format: String, vararg args: Any): Builder =
      addMember(CodeBlock.of(format, *args))

    public fun addMember(codeBlock: CodeBlock): Builder = apply {
      members += codeBlock
    }

    public fun useSiteTarget(useSiteTarget: UseSiteTarget?): Builder = apply {
      this.useSiteTarget = useSiteTarget
    }

    public fun build(): AnnotationSpec = AnnotationSpec(this)

    public companion object {
      /**
       * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
       * Handles a number of special cases, such as appending "f" to `Float` values, and uses
       * `%L` for other types.
       */
      internal fun memberForValue(value: Any) = when (value) {
        is Annotation -> CodeBlock.of("%L", get(value))
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
  @OptIn(DelicateKotlinPoetApi::class)
  private class Visitor(
    val builder: CodeBlock.Builder,
  ) : SimpleAnnotationValueVisitor8<CodeBlock.Builder, String>(builder) {

    override fun defaultAction(o: Any, name: String) =
      builder.add(Builder.memberForValue(o))

    override fun visitAnnotation(a: AnnotationMirror, name: String) =
      builder.add("%L", get(a))

    override fun visitEnumConstant(c: VariableElement, name: String) =
      builder.add("%T.%L", c.asType().asTypeName(), c.simpleName)

    override fun visitType(t: TypeMirror, name: String) =
      builder.add("%T::class", t.asTypeName())

    override fun visitArray(values: List<AnnotationValue>, name: String): CodeBlock.Builder {
      builder.add("arrayOf(⇥⇥")
      values.forEachIndexed { index, value ->
        if (index > 0) builder.add(", ")
        value.accept(this, name)
      }
      builder.add("⇤⇤)")
      return builder
    }
  }

  public companion object {
    @DelicateKotlinPoetApi(
      message = "Java reflection APIs don't give complete information on Kotlin types. Consider " +
        "using the kotlinpoet-metadata APIs instead.",
    )
    @JvmStatic
    @JvmOverloads
    public fun get(
      annotation: Annotation,
      includeDefaultValues: Boolean = false,
    ): AnnotationSpec {
      try {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val javaAnnotation = annotation as java.lang.annotation.Annotation
        val builder = builder(javaAnnotation.annotationType())
          .tag<Annotation>(annotation)
        val methods = annotation.annotationType().declaredMethods.sortedBy { it.name }
        for (method in methods) {
          val value = method.invoke(annotation)
          if (!includeDefaultValues) {
            if (Objects.deepEquals(value, method.defaultValue)) {
              continue
            }
          }
          val member = CodeBlock.builder()
          member.add("%L = ", method.name)
          if (value.javaClass.isArray) {
            member.add("arrayOf(⇥⇥")
            for (i in 0..<Array.getLength(value)) {
              if (i > 0) member.add(", ")
              member.add(Builder.memberForValue(Array.get(value, i)))
            }
            member.add("⇤⇤)")
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

    @DelicateKotlinPoetApi(
      message = "Mirror APIs don't give complete information on Kotlin types. Consider using" +
        " the kotlinpoet-metadata APIs instead.",
    )
    @JvmStatic
    public fun get(annotation: AnnotationMirror): AnnotationSpec {
      val element = annotation.annotationType.asElement() as TypeElement
      val builder = builder(element.asClassName()).tag(annotation)
      for (executableElement in annotation.elementValues.keys) {
        val member = CodeBlock.builder()
        val visitor = Visitor(member)
        val name = executableElement.simpleName.toString()
        member.add("%L = ", name)
        val value = annotation.elementValues[executableElement]!!
        value.accept(visitor, name)
        builder.addMember(member.build())
      }
      return builder.build()
    }

    @JvmStatic public fun builder(type: ClassName): Builder = Builder(type)

    @JvmStatic public fun builder(type: ParameterizedTypeName): Builder = Builder(type)

    @DelicateKotlinPoetApi(
      message = "Java reflection APIs don't give complete information on Kotlin types. Consider " +
        "using the kotlinpoet-metadata APIs instead.",
    )
    @JvmStatic
    public fun builder(type: Class<out Annotation>): Builder =
      builder(type.asClassName())

    @JvmStatic public fun builder(type: KClass<out Annotation>): Builder =
      builder(type.asClassName())
  }
}
