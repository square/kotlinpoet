/*
 * Copyright (C) 2024 Square, Inc.
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
@file:JvmName("AnnotationSpecs")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.AnnotationSpec.Companion.builder
import com.squareup.kotlinpoet.jvm.alias.JvmAnnotationMirror
import com.squareup.kotlinpoet.jvm.alias.JvmTypeElement
import java.util.Objects
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor8

internal actual fun resolveEnumValueCodeBlock(value: Enum<*>): CodeBlock =
  CodeBlock.of("%T.%L", value.javaClass, value.name)

/**
 * Annotation value visitor adding members to the given builder instance.
 */
@OptIn(DelicateKotlinPoetApi::class)
private class AnnotationSpecVisitor(
  val builder: CodeBlock.Builder,
) : SimpleAnnotationValueVisitor8<CodeBlock.Builder, String>(builder) {

  override fun defaultAction(o: Any, name: String) =
    builder.add(AnnotationSpec.Builder.memberForValue(o))

  override fun visitAnnotation(a: AnnotationMirror, name: String) =
    builder.add("%L", AnnotationSpec.get(a))

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

internal actual fun doGet(
  annotation: Annotation,
  includeDefaultValues: Boolean,
): AnnotationSpec {
  try {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val javaAnnotation = annotation as java.lang.annotation.Annotation
    val builder = builder(javaAnnotation.annotationType())
      .tag(annotation)
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
        for (i in 0..<java.lang.reflect.Array.getLength(value)) {
          if (i > 0) member.add(", ")
          member.add(AnnotationSpec.Builder.memberForValue(java.lang.reflect.Array.get(value, i)))
        }
        member.add("⇤⇤)")
        builder.addMember(member.build())
        continue
      }
      if (value is Annotation) {
        member.add("%L", AnnotationSpec.get(value))
        builder.addMember(member.build())
        continue
      }
      member.add("%L", AnnotationSpec.Builder.memberForValue(value))
      builder.addMember(member.build())
    }
    return builder.build()
  } catch (e: Exception) {
    throw RuntimeException("Reflecting $annotation failed!", e)
  }
}

internal actual fun doGet(annotation: JvmAnnotationMirror): AnnotationSpec {
  val element = annotation.annotationType.asElement() as JvmTypeElement
  val builder = builder(element.asClassName()).tag(annotation)
  for (executableElement in annotation.elementValues.keys) {
    val member = CodeBlock.builder()
    val visitor = AnnotationSpecVisitor(member)
    val name = executableElement.simpleName.toString()
    member.add("%L = ", name)
    val value = annotation.elementValues[executableElement]!!
    value.accept(visitor, name)
    builder.addMember(member.build())
  }
  return builder.build()
}
