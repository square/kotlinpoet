/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName

/**
 * Returns an [AnnotationSpec] representation of this [KSAnnotation] instance.
 * @param omitDefaultValues omit defining default values when `true`
 */
@JvmOverloads
public fun KSAnnotation.toAnnotationSpec(omitDefaultValues: Boolean = false): AnnotationSpec {
  val builder = when (val type = annotationType.resolve().unwrapTypeAlias().toTypeName()) {
    is ClassName -> AnnotationSpec.builder(type)
    is ParameterizedTypeName -> AnnotationSpec.builder(type)
    else -> error("This is never possible.")
  }
  useSiteTarget?.let { builder.useSiteTarget(it.kpAnalog) }
  // TODO support type params once they're exposed https://github.com/google/ksp/issues/753
  for (argument in arguments) {
    val value = argument.value ?: continue
    val name = argument.name!!.getShortName()
    if (omitDefaultValues) {
      val defaultValue = this.defaultArguments.firstOrNull { it.name?.asString() == name }?.value
      if (isDefaultValue(value, defaultValue)) { continue }
    }
    val member = CodeBlock.builder()
    member.add("%N = ", name)
    addValueToBlock(value, member, omitDefaultValues)
    builder.addMember(member.build())
  }
  return builder.build()
}

private fun isDefaultValue(value: Any?, defaultValue: Any?): Boolean {
  if (defaultValue == null) return false
  if (value is KSAnnotation && defaultValue is KSAnnotation) {
    return defaultValue.defaultArguments.all { defaultValueArg ->
      isDefaultValue(value.arguments.firstOrNull { it.name == defaultValueArg.name }?.value, defaultValueArg.value)
    }
  }
  if (value is List<*> && defaultValue is List<*>) {
    return value.size == defaultValue.size && defaultValue.indices.all { index ->
      isDefaultValue(value[index], defaultValue[index])
    }
  }
  return value == defaultValue
}

private val AnnotationUseSiteTarget.kpAnalog: UseSiteTarget get() = when (this) {
  AnnotationUseSiteTarget.FILE -> UseSiteTarget.FILE
  AnnotationUseSiteTarget.PROPERTY -> UseSiteTarget.PROPERTY
  AnnotationUseSiteTarget.FIELD -> UseSiteTarget.FIELD
  AnnotationUseSiteTarget.GET -> UseSiteTarget.GET
  AnnotationUseSiteTarget.SET -> UseSiteTarget.SET
  AnnotationUseSiteTarget.RECEIVER -> UseSiteTarget.RECEIVER
  AnnotationUseSiteTarget.PARAM -> UseSiteTarget.PARAM
  AnnotationUseSiteTarget.SETPARAM -> UseSiteTarget.SETPARAM
  AnnotationUseSiteTarget.DELEGATE -> UseSiteTarget.DELEGATE
}

internal fun KSType.unwrapTypeAlias(): KSType {
  return if (this.declaration is KSTypeAlias) {
    (this.declaration as KSTypeAlias).type.resolve()
  } else {
    this
  }
}

private fun addValueToBlock(value: Any, member: CodeBlock.Builder, omitDefaultValues: Boolean) {
  when (value) {
    is List<*> -> {
      // Array type
      val arrayType = when (value.firstOrNull()) {
        is Boolean -> "booleanArrayOf"
        is Byte -> "byteArrayOf"
        is Char -> "charArrayOf"
        is Short -> "shortArrayOf"
        is Int -> "intArrayOf"
        is Long -> "longArrayOf"
        is Float -> "floatArrayOf"
        is Double -> "doubleArrayOf"
        else -> "arrayOf"
      }
      member.add("$arrayType(⇥⇥")
      value.forEachIndexed { index, innerValue ->
        if (index > 0) member.add(", ")
        addValueToBlock(innerValue!!, member, omitDefaultValues)
      }
      member.add("⇤⇤)")
    }
    is KSType -> {
      val unwrapped = value.unwrapTypeAlias()
      val isEnum = (unwrapped.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_ENTRY
      if (isEnum) {
        val parent = unwrapped.declaration.parentDeclaration as KSClassDeclaration
        val entry = unwrapped.declaration.simpleName.getShortName()
        member.add("%T.%L", parent.toClassName(), entry)
      } else {
        member.add("%T::class", unwrapped.toClassName())
      }
    }
    is KSName ->
      member.add(
        "%T.%L",
        ClassName.bestGuess(value.getQualifier()),
        value.getShortName(),
      )
    is KSAnnotation -> member.add("%L", value.toAnnotationSpec(omitDefaultValues))
    else -> member.add(memberForValue(value))
  }
}

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
  is Double -> CodeBlock.of("%L", value)
  is Char -> CodeBlock.of("'%L'", value)
  is Byte -> CodeBlock.of("$value.toByte()")
  is Short -> CodeBlock.of("$value.toShort()")
  // Int or Boolean
  else -> CodeBlock.of("%L", value)
}
