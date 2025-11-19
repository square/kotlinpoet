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
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.CHAR_SEQUENCE
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.ENUM
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MUTABLE_SET
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_INT_ARRAY
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_LONG_ARRAY
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.U_SHORT_ARRAY

@KotlinPoetJavaPoetPreview
public fun KClassName.toJClassName(boxIfPrimitive: Boolean = false): JTypeName {
  return when (copy(nullable = false)) {
    BOOLEAN -> JTypeName.BOOLEAN.boxIfPrimitive(boxIfPrimitive || isNullable)
    BYTE,
    U_BYTE -> JTypeName.BYTE.boxIfPrimitive(boxIfPrimitive || isNullable)
    CHAR -> JTypeName.CHAR.boxIfPrimitive(boxIfPrimitive || isNullable)
    SHORT,
    U_SHORT -> JTypeName.SHORT.boxIfPrimitive(boxIfPrimitive || isNullable)
    INT,
    U_INT -> JTypeName.INT.boxIfPrimitive(boxIfPrimitive || isNullable)
    LONG,
    U_LONG -> JTypeName.LONG.boxIfPrimitive(boxIfPrimitive || isNullable)
    FLOAT -> JTypeName.FLOAT.boxIfPrimitive(boxIfPrimitive || isNullable)
    DOUBLE -> JTypeName.DOUBLE.boxIfPrimitive(boxIfPrimitive || isNullable)
    ANY -> JTypeName.OBJECT
    CHAR_SEQUENCE -> PoetInterop.CN_JAVA_CHAR_SEQUENCE
    STRING -> PoetInterop.CN_JAVA_STRING
    LIST,
    MUTABLE_LIST -> PoetInterop.CN_JAVA_LIST
    SET,
    MUTABLE_SET -> PoetInterop.CN_JAVA_SET
    MAP,
    MUTABLE_MAP -> PoetInterop.CN_JAVA_MAP
    BOOLEAN_ARRAY -> ArrayTypeName.of(JTypeName.BOOLEAN)
    BYTE_ARRAY,
    U_BYTE_ARRAY -> ArrayTypeName.of(JTypeName.BYTE)
    CHAR_ARRAY -> ArrayTypeName.of(JTypeName.CHAR)
    SHORT_ARRAY,
    U_SHORT_ARRAY -> ArrayTypeName.of(JTypeName.SHORT)
    INT_ARRAY,
    U_INT_ARRAY -> ArrayTypeName.of(JTypeName.INT)
    LONG_ARRAY,
    U_LONG_ARRAY -> ArrayTypeName.of(JTypeName.LONG)
    FLOAT_ARRAY -> ArrayTypeName.of(JTypeName.FLOAT)
    DOUBLE_ARRAY -> ArrayTypeName.of(JTypeName.DOUBLE)
    ENUM -> PoetInterop.CN_JAVA_ENUM
    else -> {
      if (simpleNames.size == 1) {
        JClassName.get(packageName, simpleName)
      } else {
        JClassName.get(packageName, simpleNames.first(), *simpleNames.drop(1).toTypedArray())
      }
    }
  }
}

@KotlinPoetJavaPoetPreview
public fun KParameterizedTypeName.toJParameterizedOrArrayTypeName(): JTypeName {
  return when (rawType) {
    ARRAY -> {
      val componentType =
        typeArguments.firstOrNull()?.toJTypeName()
          ?: throw IllegalStateException("Array with no type! $this")
      ArrayTypeName.of(componentType)
    }
    else -> {
      JParameterizedTypeName.get(
        rawType.toJClassName() as JClassName,
        *typeArguments.map { it.toJTypeName(boxIfPrimitive = true) }.toTypedArray(),
      )
    }
  }
}

@KotlinPoetJavaPoetPreview
public fun KParameterizedTypeName.toJParameterizedTypeName(): JParameterizedTypeName {
  check(rawType != ARRAY) {
    "Array type! JavaPoet arrays are a custom TypeName. Use this function only for things you know are not arrays"
  }
  return toJParameterizedOrArrayTypeName() as JParameterizedTypeName
}

@KotlinPoetJavaPoetPreview
public fun KTypeVariableName.toJTypeVariableName(): JTypeVariableName {
  return JTypeVariableName.get(
    name,
    *bounds.map { it.toJTypeName(boxIfPrimitive = true) }.toTypedArray(),
  )
}

@KotlinPoetJavaPoetPreview
public fun KWildcardTypeName.toJWildcardTypeName(): JWildcardTypeName {
  return if (this == STAR) {
    JWildcardTypeName.subtypeOf(TypeName.OBJECT)
  } else if (inTypes.size == 1) {
    JWildcardTypeName.supertypeOf(inTypes[0].toJTypeName())
  } else {
    JWildcardTypeName.subtypeOf(outTypes[0].toJTypeName())
  }
}

@KotlinPoetJavaPoetPreview
public fun KTypeName.toJTypeName(boxIfPrimitive: Boolean = false): JTypeName {
  return when (this) {
    is KClassName -> toJClassName(boxIfPrimitive)
    Dynamic -> throw IllegalStateException("Not applicable in Java!")
    // TODO should we return a ParameterizedTypeName of the KFunction?
    is LambdaTypeName -> throw IllegalStateException("Not applicable in Java!")
    is KParameterizedTypeName -> toJParameterizedOrArrayTypeName()
    is KTypeVariableName -> toJTypeVariableName()
    is KWildcardTypeName -> toJWildcardTypeName()
  }
}
