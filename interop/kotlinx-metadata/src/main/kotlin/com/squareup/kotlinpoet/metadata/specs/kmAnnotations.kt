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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.createClassName
import com.squareup.kotlinpoet.tag
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import kotlinx.metadata.KmAnnotationArgument.AnnotationValue
import kotlinx.metadata.KmAnnotationArgument.ArrayValue
import kotlinx.metadata.KmAnnotationArgument.BooleanValue
import kotlinx.metadata.KmAnnotationArgument.ByteValue
import kotlinx.metadata.KmAnnotationArgument.CharValue
import kotlinx.metadata.KmAnnotationArgument.DoubleValue
import kotlinx.metadata.KmAnnotationArgument.EnumValue
import kotlinx.metadata.KmAnnotationArgument.FloatValue
import kotlinx.metadata.KmAnnotationArgument.IntValue
import kotlinx.metadata.KmAnnotationArgument.KClassValue
import kotlinx.metadata.KmAnnotationArgument.LongValue
import kotlinx.metadata.KmAnnotationArgument.ShortValue
import kotlinx.metadata.KmAnnotationArgument.StringValue
import kotlinx.metadata.KmAnnotationArgument.UByteValue
import kotlinx.metadata.KmAnnotationArgument.UIntValue
import kotlinx.metadata.KmAnnotationArgument.ULongValue
import kotlinx.metadata.KmAnnotationArgument.UShortValue

@KotlinPoetMetadataPreview
internal fun KmAnnotation.toAnnotationSpec(): AnnotationSpec {
  val cn = createClassName(className)
  return AnnotationSpec.builder(cn)
    .apply {
      arguments.forEach { (name, arg) ->
        addMember("%L = %L", name, arg.toCodeBlock())
      }
    }
    .tag(this)
    .build()
}

@OptIn(ExperimentalUnsignedTypes::class)
@KotlinPoetMetadataPreview
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
  }
}
