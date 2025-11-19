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
package com.squareup.kotlinpoet.javapoet

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.ENUM
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING

@KotlinPoetJavaPoetPreview
public fun JClassName.toKClassName(): KClassName {
  return when (this) {
    JTypeName.BOOLEAN.box() -> BOOLEAN
    JTypeName.BYTE.box() -> BYTE
    JTypeName.CHAR.box() -> CHAR
    JTypeName.SHORT.box() -> SHORT
    JTypeName.INT.box() -> INT
    JTypeName.LONG.box() -> LONG
    JTypeName.FLOAT.box() -> FLOAT
    JTypeName.DOUBLE.box() -> DOUBLE
    JTypeName.OBJECT -> ANY
    PoetInterop.CN_JAVA_CHAR_SEQUENCE -> CHAR_SEQUENCE
    PoetInterop.CN_JAVA_STRING -> STRING
    PoetInterop.CN_JAVA_LIST -> LIST
    PoetInterop.CN_JAVA_SET -> SET
    PoetInterop.CN_JAVA_MAP -> MAP
    PoetInterop.CN_JAVA_ENUM -> ENUM
    else -> {
      if (simpleNames().size == 1) {
        KClassName(packageName(), simpleName())
      } else {
        KClassName(packageName(), simpleNames().first(), *simpleNames().drop(1).toTypedArray())
      }
    }
  }
}

@KotlinPoetJavaPoetPreview
public fun JParameterizedTypeName.toKParameterizedTypeName(): KParameterizedTypeName {
  return rawType
    .toKClassName()
    .parameterizedBy(*typeArguments.map { it.toKTypeName() }.toTypedArray())
}

@KotlinPoetJavaPoetPreview
public fun JTypeVariableName.toKTypeVariableName(): KTypeVariableName {
  return if (bounds.isEmpty()) {
    KTypeVariableName(name)
  } else {
    KTypeVariableName(name, *bounds.map { it.toKTypeName() }.toTypedArray())
  }
}

@KotlinPoetJavaPoetPreview
public fun JWildcardTypeName.toKWildcardTypeName(): KWildcardTypeName {
  return if (lowerBounds.size == 1) {
    KWildcardTypeName.consumerOf(lowerBounds.first().toKTypeName())
  } else {
    when (val upperBound = upperBounds[0]) {
      TypeName.OBJECT -> STAR
      else -> KWildcardTypeName.producerOf(upperBound.toKTypeName())
    }
  }
}

@KotlinPoetJavaPoetPreview
public fun JTypeName.toKTypeName(): KTypeName {
  return when (this) {
    is JClassName -> toKClassName()
    is JParameterizedTypeName -> toKParameterizedTypeName()
    is JTypeVariableName -> toKTypeVariableName()
    is JWildcardTypeName -> toKWildcardTypeName()
    is ArrayTypeName -> {
      when (componentType) {
        JTypeName.BYTE -> BYTE_ARRAY
        JTypeName.CHAR -> CHAR_ARRAY
        JTypeName.SHORT -> SHORT_ARRAY
        JTypeName.INT -> INT_ARRAY
        JTypeName.LONG -> LONG_ARRAY
        JTypeName.FLOAT -> FLOAT_ARRAY
        JTypeName.DOUBLE -> DOUBLE_ARRAY
        else -> ARRAY.parameterizedBy(componentType.toKTypeName())
      }
    }
    else ->
      when (unboxIfBoxedPrimitive()) {
        JTypeName.BOOLEAN -> BOOLEAN
        JTypeName.BYTE -> BYTE
        JTypeName.CHAR -> CHAR
        JTypeName.SHORT -> SHORT
        JTypeName.INT -> INT
        JTypeName.LONG -> LONG
        JTypeName.FLOAT -> FLOAT
        JTypeName.DOUBLE -> DOUBLE
        else -> error("Unrecognized type $this")
      }
  }
}

@OptIn(KotlinPoetJavaPoetPreview::class)
internal fun JTypeName.unboxIfBoxedPrimitive(): JTypeName {
  return if (isBoxedPrimitive) {
    unbox()
  } else {
    this
  }
}

@OptIn(KotlinPoetJavaPoetPreview::class)
internal fun JTypeName.boxIfPrimitive(extraCondition: Boolean = true): JTypeName {
  return if (extraCondition && isPrimitive && !isBoxedPrimitive) {
    box()
  } else {
    this
  }
}
