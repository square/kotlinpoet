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
package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.createClassName
import com.squareup.kotlinpoet.tag
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmAnnotationArgument.AnnotationValue
import kotlin.metadata.KmAnnotationArgument.ArrayValue
import kotlin.metadata.KmAnnotationArgument.BooleanValue
import kotlin.metadata.KmAnnotationArgument.ByteValue
import kotlin.metadata.KmAnnotationArgument.CharValue
import kotlin.metadata.KmAnnotationArgument.DoubleValue
import kotlin.metadata.KmAnnotationArgument.EnumValue
import kotlin.metadata.KmAnnotationArgument.FloatValue
import kotlin.metadata.KmAnnotationArgument.IntValue
import kotlin.metadata.KmAnnotationArgument.KClassValue
import kotlin.metadata.KmAnnotationArgument.LongValue
import kotlin.metadata.KmAnnotationArgument.ShortValue
import kotlin.metadata.KmAnnotationArgument.StringValue
import kotlin.metadata.KmAnnotationArgument.UByteValue
import kotlin.metadata.KmAnnotationArgument.UIntValue
import kotlin.metadata.KmAnnotationArgument.ULongValue
import kotlin.metadata.KmAnnotationArgument.UShortValue

internal fun KmAnnotation.toAnnotationSpec(): AnnotationSpec {
  val cn = createClassName(className)
  return AnnotationSpec.builder(cn)
    .apply { arguments.forEach { (name, arg) -> addMember("%L = %L", name, arg.toCodeBlock()) } }
    .tag(this)
    .build()
}

internal fun KmAnnotationArgument.toCodeBlock(): CodeBlock {
  return when (this) {
    is ByteValue -> CodeBlock.of("%L", value)
    is CharValue -> CodeBlock.of("'%L'", value)
    is ShortValue -> CodeBlock.of("%L", value)
    is IntValue -> CodeBlock.of("%L", value)
    is LongValue -> CodeBlock.of("%LL", value)
    is FloatValue -> CodeBlock.of("%LF", value)
    is DoubleValue -> CodeBlock.of("%L", value)
    is BooleanValue -> CodeBlock.of("%L", value)
    is UByteValue -> CodeBlock.of("%Lu", value)
    is UShortValue -> CodeBlock.of("%Lu", value)
    is UIntValue -> CodeBlock.of("%Lu", value)
    is ULongValue -> CodeBlock.of("%Lu", value)
    is StringValue -> CodeBlock.of("%S", value)
    is KClassValue -> CodeBlock.of("%T::class", createClassName(className))
    is EnumValue -> CodeBlock.of("%T.%L", createClassName(enumClassName), enumEntryName)
    is AnnotationValue -> CodeBlock.of("%L", annotation.toAnnotationSpec())
    is ArrayValue -> elements.map { it.toCodeBlock() }.joinToCode(", ", "[", "]")
    is KmAnnotationArgument.ArrayKClassValue ->
      buildCodeBlock {
        repeat(arrayDimensionCount) { add("%T<", ARRAY) }
        add("%T::class", createClassName(className))
        repeat(arrayDimensionCount) { add(">") }
      }
  }
}
