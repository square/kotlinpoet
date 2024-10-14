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

import kotlin.reflect.KClass

@Suppress("Unsupported") // Suppress Unsupported [This reflection API is not supported yet in JavaScript]
internal actual fun KClass<*>.qualifiedNameInternal(): String? {
  val nonJvmQualifier = qualifiedNameInternalNonJvm()
  if (nonJvmQualifier != null) return nonJvmQualifier

  return when (this) {
    Boolean.Companion::class -> "kotlin.Boolean.Companion"
    Byte.Companion::class -> "kotlin.Byte.Companion"
    Char.Companion::class -> "kotlin.Char.Companion"
    Double.Companion::class -> "kotlin.Double.Companion"
    Enum.Companion::class -> "kotlin.Enum.Companion"
    Float.Companion::class -> "kotlin.Float.Companion"
    Int.Companion::class -> "kotlin.Int.Companion"
    Long.Companion::class -> "kotlin.Long.Companion"
    Short.Companion::class -> "kotlin.Short.Companion"
    String.Companion::class -> "kotlin.String.Companion"
    else -> qualifiedName
  }
}
