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
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING

public fun KClassName.toJClassName(boxIfPrimitive: Boolean = false): JTypeName {
  return when (copy(nullable = false)) {
    BOOLEAN -> JTypeName.BOOLEAN.boxIfPrimitive(boxIfPrimitive || isNullable)
    BYTE -> JTypeName.BYTE.boxIfPrimitive(boxIfPrimitive || isNullable)
    CHAR -> JTypeName.CHAR.boxIfPrimitive(boxIfPrimitive || isNullable)
    SHORT -> JTypeName.SHORT.boxIfPrimitive(boxIfPrimitive || isNullable)
    INT -> JTypeName.INT.boxIfPrimitive(boxIfPrimitive || isNullable)
    LONG -> JTypeName.LONG.boxIfPrimitive(boxIfPrimitive || isNullable)
    FLOAT -> JTypeName.FLOAT.boxIfPrimitive(boxIfPrimitive || isNullable)
    DOUBLE -> JTypeName.DOUBLE.boxIfPrimitive(boxIfPrimitive || isNullable)
    ANY -> JTypeName.OBJECT
    STRING -> PoetInterop.CN_JAVA_STRING
    LIST -> PoetInterop.CN_JAVA_LIST
    SET -> PoetInterop.CN_JAVA_SET
    MAP -> PoetInterop.CN_JAVA_MAP
    else -> {
      if (simpleNames.size == 1) {
        JClassName.get(packageName, simpleName)
      } else {
        JClassName.get(packageName, simpleNames.first(), *simpleNames.drop(1).toTypedArray())
      }
    }
  }
}

public fun KParameterizedTypeName.toJParameterizedOrArrayTypeName(): JTypeName {
  return when (rawType) {
    ARRAY -> {
      val componentType = typeArguments.firstOrNull()?.toJTypeName()
        ?: throw IllegalStateException("Array with no type! $this")
      ArrayTypeName.of(componentType)
    }
    else -> {
      JParameterizedTypeName.get(
        rawType.toJClassName() as JClassName,
        *typeArguments.map { it.toJTypeName(boxIfPrimitive = true) }.toTypedArray()
      )
    }
  }
}

public fun KParameterizedTypeName.toJParameterizedTypeName(): JParameterizedTypeName {
  check(rawType != ARRAY) {
    "Array type! JavaPoet arrays are a custom TypeName. Use this function only for things you know are not arrays"
  }
  return toJParameterizedOrArrayTypeName() as JParameterizedTypeName
}

public fun KTypeVariableName.toJTypeVariableName(): JTypeVariableName {
  return JTypeVariableName.get(name, *bounds.map { it.toJTypeName(boxIfPrimitive = true) }.toTypedArray())
}

public fun KTypeName.toJTypeName(boxIfPrimitive: Boolean = false): JTypeName {
  return when (this) {
    is KClassName -> toJClassName(boxIfPrimitive)
    Dynamic -> throw IllegalStateException("Not applicable in Java!")
    // TODO should we return a ParameterizedTypeName of the KFunction?
    is LambdaTypeName -> throw IllegalStateException("Not applicable in Java!")
    is KParameterizedTypeName -> toJParameterizedOrArrayTypeName()
    is KTypeVariableName -> toJTypeVariableName()
    is KWildcardTypeName -> TODO()
  }
}
