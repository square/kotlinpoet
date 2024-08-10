/*
 * Copyright (C) 2014 Google, Inc.
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

import com.squareup.kotlinpoet.jvm.alias.JvmClass
import com.squareup.kotlinpoet.jvm.alias.JvmTypeElement
import com.squareup.kotlinpoet.jvm.alias.kotlin
import kotlin.reflect.KClass

@JvmTypeAliasKotlinPoetApi
@DelicateKotlinPoetApi(
  message = "Java reflection APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
public actual fun JvmClass<*>.asClassName(): ClassName =
  kotlin.asClassName()

@JvmTypeAliasKotlinPoetApi
@DelicateKotlinPoetApi(
  message = "Java reflection APIs don't give complete information on Kotlin types. Consider using" +
    " the kotlinpoet-metadata APIs instead.",
)
public actual fun JvmTypeElement.asClassName(): ClassName =
  throw UnsupportedOperationException()

internal actual fun Enum<*>.declaringClassName(): ClassName =
  this::class.asClassName()

internal fun KClass<*>.qualifiedNameInternalNonJvm(): String? {
  return when (this) {
    Any::class -> "kotlin.Any"
    Number::class -> "kotlin.Number"
    Boolean::class -> "kotlin.Boolean"
    Byte::class -> "kotlin.Byte"
    Char::class -> "kotlin.Char"
    Double::class -> "kotlin.Double"
    Enum::class -> "kotlin.Enum"
    Float::class -> "kotlin.Float"
    Int::class -> "kotlin.Int"
    Long::class -> "kotlin.Long"
    Short::class -> "kotlin.Short"
    String::class -> "kotlin.String"

    else -> null
  }
}
