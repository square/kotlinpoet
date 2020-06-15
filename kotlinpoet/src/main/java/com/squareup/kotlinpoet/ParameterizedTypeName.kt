/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("ParameterizedTypeNames")

package com.squareup.kotlinpoet

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

/** Returns a parameterized type equivalent to `type`.  */
@JvmName("get")
public fun ParameterizedType.asParameterizedTypeName(): ParameterizedTypeName =
    ParameterizedTypeName.get(this, mutableMapOf())

/** Returns a class name equivalent to given Kotlin KType.  */
public fun KType.asTypeName(): TypeName {
  val classifier = this.classifier
  if (classifier is KTypeParameter) {
    return classifier.asTypeVariableName().run { if (isMarkedNullable) copy(nullable = true) else this }
  }

  if (classifier == null || classifier !is KClass<*>) {
    throw IllegalArgumentException("Cannot build TypeName for $this")
  }

  return ParameterizedTypeName.get(classifier, this.isMarkedNullable, this.arguments)
}
