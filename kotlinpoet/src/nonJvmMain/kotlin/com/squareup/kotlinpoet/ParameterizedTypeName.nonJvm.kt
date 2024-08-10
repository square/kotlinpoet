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
package com.squareup.kotlinpoet

import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

internal actual fun ParameterizedTypeName.Companion.get(
  type: KClass<*>,
  nullable: Boolean,
  typeArguments: List<KTypeProjection>,
): TypeName {
  if (typeArguments.isEmpty()) {
    return type.asTypeName().run { if (nullable) copy(nullable = true) else this }
  }

  val effectiveType = if (type.isInstance(Array(0) {})) Array::class else type
  val enclosingClass: KClass<*>? = effectiveType.enclosingClass() // type.java.enclosingClass?.kotlin

  return ParameterizedTypeName(
    enclosingClass?.let {
      get(it, false, typeArguments) // .drop(effectiveType.typeParameters.size)
    },
    effectiveType.asTypeName(),
    typeArguments /* .take(effectiveType.typeParameters.size) */.map { (paramVariance, paramType) ->
      val typeName = paramType?.asTypeName() ?: return@map STAR
      when (paramVariance) {
        null -> STAR
        KVariance.INVARIANT -> typeName
        KVariance.IN -> WildcardTypeName.consumerOf(typeName)
        KVariance.OUT -> WildcardTypeName.producerOf(typeName)
      }
    },
    nullable,
    // effectiveType.annotations.map { AnnotationSpec.get(it) },
  )
}
