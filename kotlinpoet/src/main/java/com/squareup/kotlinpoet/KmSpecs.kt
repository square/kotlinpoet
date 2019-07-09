/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.km.ImmutableKmClass
import com.squareup.kotlinpoet.km.ImmutableKmConstructor
import com.squareup.kotlinpoet.km.ImmutableKmType
import com.squareup.kotlinpoet.km.ImmutableKmTypeProjection
import com.squareup.kotlinpoet.km.KotlinPoetKm
import com.squareup.kotlinpoet.km.isNullable
import com.squareup.kotlinpoet.km.isPrimary
import com.squareup.kotlinpoet.km.isSuspend
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmClassifier.TypeAlias
import kotlinx.metadata.KmClassifier.TypeParameter
import kotlinx.metadata.KmVariance
import kotlinx.metadata.KmVariance.IN
import kotlinx.metadata.KmVariance.INVARIANT
import kotlinx.metadata.KmVariance.OUT

@KotlinPoetKm
internal val ImmutableKmClass.primaryConstructor: ImmutableKmConstructor?
  get() = constructors.find { it.isPrimary }

internal fun KmVariance.asKModifier(): KModifier? {
  return when (this) {
    IN -> KModifier.IN
    OUT -> KModifier.OUT
    INVARIANT -> null
  }
}

@KotlinPoetKm
internal fun ImmutableKmTypeProjection.asTypeName(
    typeParamResolver: ((index: Int) -> TypeName)
): TypeName {
  val typename = type?.asTypeName(typeParamResolver) ?: STAR
  return when (variance) {
    IN -> WildcardTypeName.consumerOf(typename)
    OUT -> {
      if (typename == ANY) {
        // This becomes a *, which we actually don't want here.
        // List<Any> works with List<*>, but List<*> doesn't work with List<Any>
        typename
      } else {
        WildcardTypeName.producerOf(typename)
      }
    }
    INVARIANT -> typename
    null -> STAR
  }
}

@KotlinPoetKm
internal fun ImmutableKmType.asTypeName(
    typeParamResolver: ((index: Int) -> TypeName),
    useTypeAlias: Boolean = false
): TypeName {
  val argumentList = arguments.map { it.asTypeName(typeParamResolver) }
  val type: TypeName = when (val valClassifier = classifier) {
    is TypeParameter -> {
      typeParamResolver(valClassifier.id)
    }
    is KmClassifier.Class -> {
      flexibleTypeUpperBound?.asTypeName(typeParamResolver)?.let { return it }
      outerType?.asTypeName(typeParamResolver)?.let { return it }
      var finalType: TypeName = ClassName.bestGuess(valClassifier.name.replace("/", "."))
      if (argumentList.isNotEmpty()) {
        val finalTypeString = finalType.toString()
        if (finalTypeString.startsWith("kotlin.Function")) {
          // It's a lambda type!
          finalType = if (finalTypeString == "kotlin.FunctionN") {
            TODO("unclear how to express this one since it has arity")
          } else {
            val (parameters, returnType) = if (isSuspend) {
              // Coroutines always adds an `Any?` return type, but we kind of just want the
              // source representation, so we trick it here and ignore the last.
              argumentList.dropLast(2).toTypedArray() to argumentList.dropLast(1).last().let {
                // Coroutines makes these a `Continuation<T>` of the type, so we want the parameterized type
                check(it is ParameterizedTypeName)
                it.typeArguments[0]
              }
            } else {
              argumentList.dropLast(1).toTypedArray() to argumentList.last()
            }
            val lambdaType = if (isExtensionType) {
              // Extension function type! T.(). First parameter is actually the receiver.
              LambdaTypeName.get(
                  receiver = parameters[0],
                  parameters = *parameters.drop(1).toTypedArray(),
                  returnType = returnType
              )
            } else {
              LambdaTypeName.get(
                  receiver = null,
                  parameters = *parameters,
                  returnType = returnType
              )
            }
            lambdaType.copy(suspending = isSuspend)
          }
        } else {
          finalType = (finalType as ClassName).parameterizedBy(*argumentList.toTypedArray())
        }
      }
      finalType
    }
    is TypeAlias -> {
      if (useTypeAlias) {
        ClassName.bestGuess(valClassifier.name)
      } else {
        checkNotNull(abbreviatedType).asTypeName(typeParamResolver)
      }
    }
  }

  return type.copy(nullable = isNullable)
}
